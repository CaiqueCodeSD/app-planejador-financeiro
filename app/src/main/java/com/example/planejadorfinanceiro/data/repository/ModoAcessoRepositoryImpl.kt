package com.example.planejadorfinanceiro.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.planejadorfinanceiro.domain.model.ModoAcesso
import com.example.planejadorfinanceiro.domain.repository.ModoAcessoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ModoAcessoRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : ModoAcessoRepository {

    companion object {
        private val MODO_ACESSO_KEY = stringPreferencesKey("modo_acesso")
    }

    override fun observarModoAcesso(): Flow<ModoAcesso> {
        return dataStore.data.map { preferences ->
            val valorSalvo = preferences[MODO_ACESSO_KEY]

            runCatching {
                valorSalvo?.let { ModoAcesso.valueOf(it) }
                    ?: ModoAcesso.NAO_CONFIGURADO
            }.getOrDefault(ModoAcesso.NAO_CONFIGURADO)
        }
    }

    override suspend fun salvarModoAcesso(modo: ModoAcesso) {
        dataStore.edit { preferences ->
            preferences[MODO_ACESSO_KEY] = modo.name
        }
    }
}