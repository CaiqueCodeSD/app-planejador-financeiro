package com.example.planejadorfinanceiro.domain.repository

interface GoogleSheetsRepository {

    suspend fun criarPlanilha(
        accessToken: String,
        titulo: String
    ): String

    suspend fun configurarCabecalhos(
        accessToken: String,
        spreadsheetId: String
    )
}
