package com.example.planejadorfinanceiro.domain.repository

import kotlinx.coroutines.flow.Flow

sealed interface ResultadoValidacaoPin {
    data object Valido : ResultadoValidacaoPin
    data object Incorreto : ResultadoValidacaoPin
    data class Bloqueado(
        val tempoRestanteMillis: Long
    ) : ResultadoValidacaoPin
}

interface PinRepository {

    fun observarPinAtivado(): Flow<Boolean>

    suspend fun cadastrarPin(pin: String)

    suspend fun validarPin(pin: String): ResultadoValidacaoPin

    suspend fun alterarPin(
        pinAtual: String,
        novoPin: String
    ): ResultadoValidacaoPin

    suspend fun desativarPin(
        pinAtual: String
    ): ResultadoValidacaoPin
}