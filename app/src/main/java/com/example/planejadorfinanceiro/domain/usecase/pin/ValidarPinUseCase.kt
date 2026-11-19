package com.example.planejadorfinanceiro.domain.usecase.pin

import com.example.planejadorfinanceiro.domain.repository.PinRepository
import com.example.planejadorfinanceiro.domain.repository.ResultadoValidacaoPin

class ValidarPinUseCase(
    private val pinRepository: PinRepository
) {

    suspend operator fun invoke(pin: String): ResultadoValidacaoPin {
        return pinRepository.validarPin(pin)
    }
}