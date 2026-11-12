package com.example.planejadorfinanceiro.domain.repository

import kotlinx.coroutines.flow.Flow

interface GoogleSheetsConfigRepository {

    fun observarSpreadsheetId(): Flow<String?>

    suspend fun salvarSpreadsheetId(id: String)
}
