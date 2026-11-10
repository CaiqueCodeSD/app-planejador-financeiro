
package com.example.planejadorfinanceiro.data.repository

import com.example.planejadorfinanceiro.data.remote.GoogleSheetsHttpClient
import com.example.planejadorfinanceiro.domain.repository.GoogleSheetsRepository
import org.json.JSONArray
import org.json.JSONObject
import com.example.planejadorfinanceiro.data.remote.GoogleSheetsSchema


class GoogleSheetsRepositoryImpl(
    private val httpClient: GoogleSheetsHttpClient
) : GoogleSheetsRepository {


    override suspend fun configurarCabecalhos(
        accessToken: String,
        spreadsheetId: String
    ) {
        val dados = JSONArray().apply {
            put(criarEntradaCabecalho(
                GoogleSheetsSchema.ABA_CONTAS,
                GoogleSheetsSchema.CABECALHO_CONTAS
            ))

            put(criarEntradaCabecalho(
                GoogleSheetsSchema.ABA_MOVIMENTACOES,
                GoogleSheetsSchema.CABECALHO_MOVIMENTACOES
            ))
        }

        val corpo = JSONObject().apply {
            put("valueInputOption", "RAW")
            put("data", dados)
        }

        httpClient.executar(
            url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values:batchUpdate",
            accessToken = accessToken,
            metodo = "POST",
            json = corpo.toString()
        )
    }

    private fun criarEntradaCabecalho(
        aba: String,
        cabecalhos: List<String>
    ): JSONObject {
        return JSONObject().apply {
            put("range", "'$aba'!A1")
            put("majorDimension", "ROWS")
            put("values", JSONArray().put(JSONArray(cabecalhos)))
        }
    }


    override suspend fun criarPlanilha(
        accessToken: String,
        titulo: String
    ): String {

        val abas = JSONArray().apply {
            put(criarAba("Contas"))
            put(criarAba("Movimentações"))
        }

        val corpo = JSONObject().apply {
            put(
                "properties",
                JSONObject().apply {
                    put("title", titulo)
                }
            )

            put("sheets", abas)
        }

        val resposta = httpClient.executar(
            url = "https://sheets.googleapis.com/v4/spreadsheets",
            accessToken = accessToken,
            metodo = "POST",
            json = corpo.toString()
        )

        return JSONObject(resposta)
            .getString("spreadsheetId")
    }

    private fun criarAba(titulo: String): JSONObject {
        return JSONObject().apply {
            put(
                "properties",
                JSONObject().apply {
                    put("title", titulo)
                }
            )
        }
    }
}
