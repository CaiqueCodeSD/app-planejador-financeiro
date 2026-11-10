
package com.example.planejadorfinanceiro.data.repository
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.planejadorfinanceiro.domain.repository.GoogleSheetsConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoogleSheetsConfigRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : GoogleSheetsConfigRepository {

    companion object {
        private val SPREADSHEET_ID_KEY =
            stringPreferencesKey("google_spreadsheet_id")
    }

    override fun observarSpreadsheetId(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[SPREADSHEET_ID_KEY]
        }
    }

    override suspend fun salvarSpreadsheetId(id: String) {
        require(id.isNotBlank()) {
            "O ID da planilha não pode estar vazio."
        }

        dataStore.edit { preferences ->
            preferences[SPREADSHEET_ID_KEY] = id
        }
    }
}
