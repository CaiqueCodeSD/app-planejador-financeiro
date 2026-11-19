package com.example.planejadorfinanceiro.domain.usecase.pin

import com.example.planejadorfinanceiro.domain.repository.PinRepository
import com.example.planejadorfinanceiro.domain.repository.ResultadoValidacaoPin

class GerenciarPinUseCase(
    private val pinRepository: PinRepository
) {

    suspend fun alterarPin(
        pinAtual: String,
        novoPin: String
    ): ResultadoValidacaoPin {
        return pinRepository.alterarPin(pinAtual, novoPin)
    }

    suspend fun desativarPin(
        pinAtual: String
    ): ResultadoValidacaoPin {
        return pinRepository.desativarPin(pinAtual)
    }
}