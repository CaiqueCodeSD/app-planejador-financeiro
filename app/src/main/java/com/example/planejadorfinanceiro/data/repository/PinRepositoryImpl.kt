package com.example.planejadorfinanceiro.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.planejadorfinanceiro.data.security.MaterialPinProtegido
import com.example.planejadorfinanceiro.data.security.PinClock
import com.example.planejadorfinanceiro.data.security.PinCriptografia
import com.example.planejadorfinanceiro.data.security.PinMaterialProtector
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
    private val materialProtector: PinMaterialProtector,
    private val pinClock: PinClock
) : PinRepository {

    companion object {
        private val PIN_ATIVADO =
            booleanPreferencesKey("pin_ativado")

        private val PIN_MATERIAL_IV =
            stringPreferencesKey("pin_material_iv")

        private val PIN_MATERIAL_CONTEUDO =
            stringPreferencesKey("pin_material_conteudo")

        private val PIN_TENTATIVAS =
            intPreferencesKey("pin_tentativas")

        private val PIN_BLOQUEIO_ATE =
            longPreferencesKey("pin_bloqueio_ate")

        private val PIN_BLOQUEIO_ATE_ELAPSED =
            longPreferencesKey("pin_bloqueio_ate_elapsed")

        private val PIN_BOOT_COUNT_BLOQUEIO =
            intPreferencesKey("pin_boot_count_bloqueio")

        private const val TAMANHO_SALT = 16
        private const val TAMANHO_HASH = 32
        private const val TAMANHO_MATERIAL =
            TAMANHO_SALT + TAMANHO_HASH

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
            val material = protegerMaterial(salt, hash)

            dataStore.edit { preferences ->
                preferences[PIN_MATERIAL_IV] = codificar(material.iv)
                preferences[PIN_MATERIAL_CONTEUDO] =
                    codificar(material.conteudo)

                preferences[PIN_ATIVADO] = true
                preferences[PIN_TENTATIVAS] = 0
                preferences[PIN_BLOQUEIO_ATE] = 0L

                preferences.remove(PIN_BLOQUEIO_ATE_ELAPSED)
                preferences.remove(PIN_BOOT_COUNT_BLOQUEIO)
            }
        }
    }

    override suspend fun validarPin(
        pin: String
    ): ResultadoValidacaoPin {
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
            val material = protegerMaterial(salt, hash)

            dataStore.edit { preferences ->
                preferences[PIN_MATERIAL_IV] = codificar(material.iv)
                preferences[PIN_MATERIAL_CONTEUDO] =
                    codificar(material.conteudo)

                preferences[PIN_TENTATIVAS] = 0
                preferences[PIN_BLOQUEIO_ATE] = 0L

                preferences.remove(PIN_BLOQUEIO_ATE_ELAPSED)
                preferences.remove(PIN_BOOT_COUNT_BLOQUEIO)
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
                preferences.remove(PIN_MATERIAL_IV)
                preferences.remove(PIN_MATERIAL_CONTEUDO)
                preferences.remove(PIN_TENTATIVAS)
                preferences.remove(PIN_BLOQUEIO_ATE)
                preferences.remove(PIN_BLOQUEIO_ATE_ELAPSED)
                preferences.remove(PIN_BOOT_COUNT_BLOQUEIO)
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

        val tempoRestante = calcularTempoRestanteBloqueio(preferences)

        if (tempoRestante > 0L) {
            return ResultadoValidacaoPin.Bloqueado(tempoRestante)
        }

        val ivArmazenado = preferences[PIN_MATERIAL_IV]
            ?: throw IllegalStateException(
                "IV do material criptográfico não encontrado."
            )

        val conteudoArmazenado =
            preferences[PIN_MATERIAL_CONTEUDO]
                ?: throw IllegalStateException(
                    "Material criptográfico do PIN não encontrado."
                )

        val pinValido = pin.matches(Regex("^\\d{4}$"))

        val correto = if (pinValido) {
            val (salt, hashArmazenado) = recuperarMaterial(
                ivArmazenado,
                conteudoArmazenado
            )

            val hashInformado = criptografia.derivarPin(
                pin,
                salt
            )

            criptografia.comparar(
                hashArmazenado,
                hashInformado
            )
        } else {
            false
        }

        if (correto) {
            dataStore.edit { prefs ->
                prefs[PIN_TENTATIVAS] = 0
                prefs[PIN_BLOQUEIO_ATE] = 0L

                prefs.remove(PIN_BLOQUEIO_ATE_ELAPSED)
                prefs.remove(PIN_BOOT_COUNT_BLOQUEIO)
            }

            return ResultadoValidacaoPin.Valido
        }

        var novoBloqueio = 0L

        dataStore.edit { prefs ->
            val tentativas = (prefs[PIN_TENTATIVAS] ?: 0) + 1

            if (tentativas >= MAX_TENTATIVAS) {
                prefs[PIN_TENTATIVAS] = 0

                novoBloqueio =
                    pinClock.obterHorarioAtual() + DURACAO_BLOQUEIO

                prefs[PIN_BLOQUEIO_ATE] = novoBloqueio

                prefs[PIN_BLOQUEIO_ATE_ELAPSED] =
                    pinClock.obterTempoDesdeInicializacao() +
                            DURACAO_BLOQUEIO

                prefs[PIN_BOOT_COUNT_BLOQUEIO] =
                    pinClock.obterContagemInicializacoes()
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

    private fun calcularTempoRestanteBloqueio(
        preferences: Preferences
    ): Long {
        val bloqueioAte = preferences[PIN_BLOQUEIO_ATE] ?: 0L

        if (bloqueioAte <= 0L) return 0L

        val bootCountArmazenado =
            preferences[PIN_BOOT_COUNT_BLOQUEIO] ?: -1

        val bootCountAtual =
            pinClock.obterContagemInicializacoes()

        val bloqueioAteElapsed =
            preferences[PIN_BLOQUEIO_ATE_ELAPSED]

        val mesmaInicializacao =
            bootCountArmazenado >= 0 &&
                    bootCountAtual >= 0 &&
                    bootCountArmazenado == bootCountAtual

        return if (mesmaInicializacao && bloqueioAteElapsed != null) {
            (
                    bloqueioAteElapsed -
                            pinClock.obterTempoDesdeInicializacao()
                    ).coerceAtLeast(0L)
        } else {
            (
                    bloqueioAte -
                            pinClock.obterHorarioAtual()
                    ).coerceAtLeast(0L)
        }
    }

    private fun validarFormato(pin: String) {
        require(pin.matches(Regex("^\\d{4}$"))) {
            "O PIN deve conter exatamente quatro dígitos."
        }
    }

    private fun protegerMaterial(
        salt: ByteArray,
        hash: ByteArray
    ): MaterialPinProtegido {
        require(salt.size == TAMANHO_SALT)
        require(hash.size == TAMANHO_HASH)

        val dados = salt + hash

        return materialProtector.proteger(dados)
    }

    private fun recuperarMaterial(
        iv: String,
        conteudo: String
    ): Pair<ByteArray, ByteArray> {
        val material = MaterialPinProtegido(
            iv = decodificar(iv),
            conteudo = decodificar(conteudo)
        )

        val dados = materialProtector.desproteger(material)

        require(dados.size == TAMANHO_MATERIAL) {
            "Material criptográfico do PIN inválido."
        }

        val salt = dados.copyOfRange(0, TAMANHO_SALT)
        val hash = dados.copyOfRange(
            TAMANHO_SALT,
            TAMANHO_MATERIAL
        )

        return salt to hash
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

        require(valor.all {
            it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F'
        }) {
            "Dados criptográficos inválidos."
        }

        return ByteArray(valor.length / 2) { index ->
            valor.substring(index * 2, index * 2 + 2)
                .toInt(16)
                .toByte()
        }
    }
}
