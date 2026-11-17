package com.example.planejadorfinanceiro.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.planejadorfinanceiro.data.security.PinCriptografia
import com.example.planejadorfinanceiro.domain.repository.PinRepository
import com.example.planejadorfinanceiro.domain.repository.ResultadoValidacaoPin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PinRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val criptografia: PinCriptografia,
    private val obterHorarioAtual: () -> Long = System::currentTimeMillis
) : PinRepository {

    companion object {
        private val PIN_ATIVADO = booleanPreferencesKey("pin_ativado")
        private val PIN_HASH = stringPreferencesKey("pin_hash")
        private val PIN_SALT = stringPreferencesKey("pin_salt")
        private val PIN_TENTATIVAS = intPreferencesKey("pin_tentativas")
        private val PIN_BLOQUEIO_ATE = longPreferencesKey("pin_bloqueio_ate")

        private const val MAX_TENTATIVAS = 5
        private const val DURACAO_BLOQUEIO = 60_000L
    }

    private val mutex = Mutex()

    override fun observarPinAtivado(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[PIN_ATIVADO] ?: false
        }
    }

    override suspend fun cadastrarPin(pin: String) {
        mutex.withLock {
            validarFormato(pin)

            val ativado = dataStore.data.first()[PIN_ATIVADO] ?: false

            check(!ativado) {
                "Já existe um PIN cadastrado."
            }

            val salt = criptografia.gerarSalt()
            val hash = criptografia.derivarPin(pin, salt)

            dataStore.edit { preferences ->
                preferences[PIN_HASH] = codificar(hash)
                preferences[PIN_SALT] = codificar(salt)
                preferences[PIN_ATIVADO] = true
                preferences[PIN_TENTATIVAS] = 0
                preferences[PIN_BLOQUEIO_ATE] = 0L
            }
        }
    }

    override suspend fun validarPin(pin: String): ResultadoValidacaoPin {
        return mutex.withLock {
            validarInternamente(pin)
        }
    }

    override suspend fun alterarPin(
        pinAtual: String,
        novoPin: String
    ): ResultadoValidacaoPin {
        return mutex.withLock {
            val resultado = validarInternamente(pinAtual)

            if (resultado != ResultadoValidacaoPin.Valido) {
                return@withLock resultado
            }

            validarFormato(novoPin)

            val salt = criptografia.gerarSalt()
            val hash = criptografia.derivarPin(novoPin, salt)

            dataStore.edit { preferences ->
                preferences[PIN_HASH] = codificar(hash)
                preferences[PIN_SALT] = codificar(salt)
                preferences[PIN_TENTATIVAS] = 0
                preferences[PIN_BLOQUEIO_ATE] = 0L
            }

            ResultadoValidacaoPin.Valido
        }
    }

    override suspend fun desativarPin(
        pinAtual: String
    ): ResultadoValidacaoPin {
        return mutex.withLock {
            val resultado = validarInternamente(pinAtual)

            if (resultado != ResultadoValidacaoPin.Valido) {
                return@withLock resultado
            }

            dataStore.edit { preferences ->
                preferences.remove(PIN_ATIVADO)
                preferences.remove(PIN_HASH)
                preferences.remove(PIN_SALT)
                preferences.remove(PIN_TENTATIVAS)
                preferences.remove(PIN_BLOQUEIO_ATE)
            }

            ResultadoValidacaoPin.Valido
        }
    }

    private suspend fun validarInternamente(
        pin: String
    ): ResultadoValidacaoPin {
        val preferences = dataStore.data.first()

        if (preferences[PIN_ATIVADO] != true) {
            return ResultadoValidacaoPin.Incorreto
        }

        val agora = obterHorarioAtual()
        val bloqueioAte = preferences[PIN_BLOQUEIO_ATE] ?: 0L

        if (agora < bloqueioAte) {
            return ResultadoValidacaoPin.Bloqueado(
                bloqueioAte - agora
            )
        }

        val hashArmazenado = preferences[PIN_HASH]
            ?: return ResultadoValidacaoPin.Incorreto

        val saltArmazenado = preferences[PIN_SALT]
            ?: return ResultadoValidacaoPin.Incorreto

        val pinValido = pin.matches(Regex("^\\d{4}$"))

        val correto = if (pinValido) {
            val hash = criptografia.derivarPin(
                pin,
                decodificar(saltArmazenado)
            )

            criptografia.comparar(
                decodificar(hashArmazenado),
                hash
            )
        } else {
            false
        }

        if (correto) {
            dataStore.edit { prefs ->
                prefs[PIN_TENTATIVAS] = 0
                prefs[PIN_BLOQUEIO_ATE] = 0L
            }

            return ResultadoValidacaoPin.Valido
        }

        var novoBloqueio = 0L

        dataStore.edit { prefs ->
            val tentativas = (prefs[PIN_TENTATIVAS] ?: 0) + 1

            if (tentativas >= MAX_TENTATIVAS) {
                prefs[PIN_TENTATIVAS] = 0
                novoBloqueio = agora + DURACAO_BLOQUEIO
                prefs[PIN_BLOQUEIO_ATE] = novoBloqueio
            } else {
                prefs[PIN_TENTATIVAS] = tentativas
            }
        }

        return if (novoBloqueio > 0L) {
            ResultadoValidacaoPin.Bloqueado(DURACAO_BLOQUEIO)
        } else {
            ResultadoValidacaoPin.Incorreto
        }
    }

    private fun validarFormato(pin: String) {
        require(pin.matches(Regex("^\\d{4}$"))) {
            "O PIN deve conter exatamente quatro dígitos."
        }
    }

    private fun codificar(bytes: ByteArray): String {
        return bytes.joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
    }

    private fun decodificar(valor: String): ByteArray {
        require(valor.length % 2 == 0) {
            "Dados criptográficos inválidos."
        }

        return ByteArray(valor.length / 2) { index ->
            valor.substring(index * 2, index * 2 + 2)
                .toInt(16)
                .toByte()
        }
    }
}
