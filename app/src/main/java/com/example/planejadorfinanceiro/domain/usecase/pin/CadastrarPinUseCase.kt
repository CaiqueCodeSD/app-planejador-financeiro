package com.example.planejadorfinanceiro.domain.usecase.pin

import com.example.planejadorfinanceiro.domain.repository.PinRepository

class CadastrarPinUseCase(
    private val pinRepository: PinRepository
) {

    suspend operator fun invoke(pin: String) {
        pinRepository.cadastrarPin(pin)
    }
}