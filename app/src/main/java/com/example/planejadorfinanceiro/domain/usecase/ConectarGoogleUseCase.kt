package com.example.planejadorfinanceiro.domain.usecase

import com.example.planejadorfinanceiro.domain.model.ModoAcesso
import com.example.planejadorfinanceiro.domain.repository.GoogleSheetsConfigRepository
import com.example.planejadorfinanceiro.domain.repository.GoogleSheetsRepository
import com.example.planejadorfinanceiro.domain.repository.ModoAcessoRepository

class ConectarGoogleUseCase(
    private val googleSheetsRepository: GoogleSheetsRepository,
    private val googleSheetsConfigRepository: GoogleSheetsConfigRepository,
    private val modoAcessoRepository: ModoAcessoRepository
) {

    suspend operator fun invoke(accessToken: String) {
        val spreadsheetId = googleSheetsRepository.criarPlanilha(
            accessToken = accessToken,
            titulo = "Planejador Financeiro"
        )

        googleSheetsRepository.configurarCabecalhos(
            accessToken = accessToken,
            spreadsheetId = spreadsheetId
        )

        googleSheetsConfigRepository.salvarSpreadsheetId(
            spreadsheetId
        )

        modoAcessoRepository.salvarModoAcesso(
            ModoAcesso.CONECTADO_GOOGLE
        )
    }
}
