package com.example.planejadorfinanceiro.data.remote

import com.example.planejadorfinanceiro.domain.exception.AutorizacaoGoogleNecessariaException
import com.example.planejadorfinanceiro.domain.repository.GoogleAuthorizationRepository
import com.example.planejadorfinanceiro.domain.repository.GoogleSheetsConfigRepository
import com.example.planejadorfinanceiro.domain.repository.ResultadoAutorizacao
import com.example.planejadorfinanceiro.domain.repository.SincronizacaoDataSource
import com.example.planejadorfinanceiro.domain.model.Conta
import com.example.planejadorfinanceiro.domain.model.Movimentacao
import com.example.planejadorfinanceiro.domain.model.ExclusaoPendente
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class GoogleSheetsSincronizacaoDataSource(
    private val authorizationRepository: GoogleAuthorizationRepository,
    private val configRepository: GoogleSheetsConfigRepository,
    private val httpClient: GoogleSheetsHttpClient
) : SincronizacaoDataSource {

    private suspend fun obterCredenciais(): Pair<String, String> {
        val resultado = authorizationRepository.obterAccessToken()

        val token = when (resultado) {
            is ResultadoAutorizacao.TokenDisponivel -> resultado.token
            ResultadoAutorizacao.RequerInteracao -> {
                throw AutorizacaoGoogleNecessariaException()
            }
        }

        val spreadsheetId = configRepository.observarSpreadsheetId().first()
            ?: throw IllegalStateException("Planilha Google não configurada.")

        return token to spreadsheetId
    }

    private suspend fun buscarLinhaPorUuid(
        accessToken: String,
        spreadsheetId: String,
        aba: String,
        uuid: String
    ): Int? {
        val intervalo = URLEncoder.encode(
            "'$aba'!A:A",
            "UTF-8"
        )

        val resposta = httpClient.executar(
            url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$intervalo",
            accessToken = accessToken
        )

        val valores = JSONObject(resposta).optJSONArray("values")
            ?: return null

        for (indice in 0 until valores.length()) {
            val linha = valores.optJSONArray(indice) ?: continue

            if (linha.optString(0) == uuid) {
                return indice + 1
            }
        }

        return null
    }

    private suspend fun inserirLinha(
        accessToken: String,
        spreadsheetId: String,
        aba: String,
        corpo: JSONObject
    ) {
        val intervalo = URLEncoder.encode(
            "'$aba'!A:A",
            "UTF-8"
        )

        httpClient.executar(
            url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$intervalo:append?valueInputOption=RAW&insertDataOption=INSERT_ROWS",
            accessToken = accessToken,
            metodo = "POST",
            json = corpo.toString()
        )
    }

    private suspend fun atualizarLinha(
        accessToken: String,
        spreadsheetId: String,
        aba: String,
        numeroLinha: Int,
        ultimaColuna: String,
        corpo: JSONObject
    ) {
        val intervalo = URLEncoder.encode(
            "'$aba'!A${numeroLinha}:${ultimaColuna}${numeroLinha}",
            "UTF-8"
        )

        httpClient.executar(
            url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$intervalo?valueInputOption=RAW",
            accessToken = accessToken,
            metodo = "PUT",
            json = corpo.toString()
        )
    }

    private suspend fun buscarSheetId(
        accessToken: String,
        spreadsheetId: String,
        aba: String
    ): Int {
        val resposta = httpClient.executar(
            url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId?fields=sheets(properties(sheetId,title))",
            accessToken = accessToken
        )

        val abas = JSONObject(resposta).optJSONArray("sheets")
            ?: throw IllegalStateException("Não foi possível consultar as abas.")

        for (indice in 0 until abas.length()) {
            val propriedades = abas
                .optJSONObject(indice)
                ?.optJSONObject("properties")
                ?: continue

            if (propriedades.optString("title") == aba) {
                return propriedades.optInt("sheetId")
            }
        }

        throw IllegalStateException("A aba '$aba' não foi encontrada.")
    }

    override suspend fun sincronizarConta(conta: Conta) {
        val (token, spreadsheetId) = obterCredenciais()

        val linhaExistente = buscarLinhaPorUuid(
            accessToken = token,
            spreadsheetId = spreadsheetId,
            aba = GoogleSheetsSchema.ABA_CONTAS,
            uuid = conta.uuid
        )

        val dados = JSONArray().put(
            JSONArray(GoogleSheetsMapper.mapearConta(conta))
        )

        val corpo = JSONObject().apply {
            put("majorDimension", "ROWS")
            put("values", dados)
        }

        if (linhaExistente != null) {
            atualizarLinha(
                accessToken = token,
                spreadsheetId = spreadsheetId,
                aba = GoogleSheetsSchema.ABA_CONTAS,
                numeroLinha = linhaExistente,
                ultimaColuna = "H",
                corpo = corpo

            )
        } else {
            inserirLinha(
                accessToken = token,
                spreadsheetId = spreadsheetId,
                aba = GoogleSheetsSchema.ABA_CONTAS,
                corpo = corpo
            )
        }
    }

    override suspend fun sincronizarMovimentacao(
        movimentacao: Movimentacao
    ) {
        val (token, spreadsheetId) = obterCredenciais()

        val aba = GoogleSheetsSchema.ABA_MOVIMENTACOES

        val linhaExistente = buscarLinhaPorUuid(
            accessToken = token,
            spreadsheetId = spreadsheetId,
            aba = aba,

            uuid = movimentacao.uuid
        )

        val dados = JSONArray().put(
            JSONArray(
                GoogleSheetsMapper.mapearMovimentacao(movimentacao)
            )
        )

        val corpo = JSONObject().apply {
            put("majorDimension", "ROWS")
            put("values", dados)
        }

        if (linhaExistente != null) {
            atualizarLinha(
                accessToken = token,
                spreadsheetId = spreadsheetId,
                aba = aba,
                numeroLinha = linhaExistente,
                ultimaColuna = "I",
                corpo = corpo
            )
        } else {
            inserirLinha(
                accessToken = token,
                spreadsheetId = spreadsheetId,
                aba = aba,
                corpo = corpo
            )
        }
    }

    override suspend fun sincronizarExclusao(
        exclusao: ExclusaoPendente
    ) {
        val (token, spreadsheetId) = obterCredenciais()

        val aba = when (exclusao.tipoRegistro) {
            "CONTA" -> GoogleSheetsSchema.ABA_CONTAS
            "MOVIMENTACAO" -> GoogleSheetsSchema.ABA_MOVIMENTACOES
            else -> throw IllegalArgumentException(
                "Tipo de registro desconhecido: ${exclusao.tipoRegistro}"
            )
        }

        val numeroLinha = buscarLinhaPorUuid(
            accessToken = token,
            spreadsheetId = spreadsheetId,
            aba = aba,
            uuid = exclusao.uuid
        ) ?: return

        val sheetId = buscarSheetId(
            accessToken = token,
            spreadsheetId = spreadsheetId,
            aba = aba
        )

        val corpo = JSONObject().apply {
            put(
                "requests",
                JSONArray().put(
                    JSONObject().apply {
                        put(
                            "deleteDimension",
                            JSONObject().apply {
                                put(
                                    "range",
                                    JSONObject().apply {
                                        put("sheetId", sheetId)
                                        put("dimension", "ROWS")
                                        put("startIndex", numeroLinha - 1)
                                        put("endIndex", numeroLinha)
                                    }
                                )
                            }
                        )
                    }
                )
            )
        }

        httpClient.executar(
            url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId:batchUpdate",
            accessToken = token,
            metodo = "POST",
            json = corpo.toString()
        )
    }
}
