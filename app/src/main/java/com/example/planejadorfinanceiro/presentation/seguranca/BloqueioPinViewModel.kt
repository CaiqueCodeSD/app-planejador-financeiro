package com.example.planejadorfinanceiro.presentation.seguranca

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planejadorfinanceiro.data.security.GerenciadorBloqueioPin
import com.example.planejadorfinanceiro.domain.repository.ResultadoValidacaoPin
import com.example.planejadorfinanceiro.domain.usecase.pin.ValidarPinUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BloqueioPinViewModel(
    private val validarPinUseCase: ValidarPinUseCase,
    private val gerenciadorBloqueioPin: GerenciadorBloqueioPin
) : ViewModel() {

    val estadoBloqueio = gerenciadorBloqueioPin.estado

    private val _resultadoValidacao =
        MutableStateFlow<ResultadoValidacaoPin?>(null)

    val resultadoValidacao: StateFlow<ResultadoValidacaoPin?> =
        _resultadoValidacao.asStateFlow()

    fun validarPin(pin: String) {
        viewModelScope.launch {
            val resultado = validarPinUseCase(pin)

            _resultadoValidacao.value = resultado

            if (resultado == ResultadoValidacaoPin.Valido) {
                gerenciadorBloqueioPin.desbloquear()
            }
        }
    }

    fun limparResultado() {
        _resultadoValidacao.value = null
    }
}
