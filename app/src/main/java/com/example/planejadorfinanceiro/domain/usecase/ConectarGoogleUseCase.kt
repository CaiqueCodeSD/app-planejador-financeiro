package com.example.planejadorfinanceiro.domain.usecase

import com.example.planejadorfinanceiro.domain.model.ModoAcesso
import com.example.planejadorfinanceiro.domain.repository.GoogleSheetsConfigRepository
import com.example.planejadorfinanceiro.domain.repository.GoogleSheetsRepository
import com.example.planejadorfinanceiro.domain.repository.ModoAcessoRepository
import kotlinx.coroutines.flow.first

class ConectarGoogleUseCase(
    private val googleSheetsRepository: GoogleSheetsRepository,
    private val googleSheetsConfigRepository: GoogleSheetsConfigRepository,
    private val modoAcessoRepository: ModoAcessoRepository
) {

    suspend operator fun invoke(accessToken: String) {

        val spreadsheetIdExistente =
            googleSheetsConfigRepository
                .observarSpreadsheetId()
                .first()

        println("USECASE -> ID EXISTENTE: $spreadsheetIdExistente")

        val spreadsheetId = if (spreadsheetIdExistente.isNullOrBlank()) {

            println("USECASE -> CRIANDO NOVA PLANILHA")

            val novoId = googleSheetsRepository.criarPlanilha(
                accessToken = accessToken,
                titulo = "Planejador Financeiro"
            )

            googleSheetsConfigRepository.salvarSpreadsheetId(novoId)

            println("USECASE -> NOVO ID SALVO: $novoId")

            novoId

        } else {

            println("USECASE -> REUTILIZANDO PLANILHA EXISTENTE")

            spreadsheetIdExistente
        }

        googleSheetsRepository.configurarCabecalhos(
            accessToken = accessToken,
            spreadsheetId = spreadsheetId
        )

        modoAcessoRepository.salvarModoAcesso(
            ModoAcesso.CONECTADO_GOOGLE
        )
    }
}
