package com.example.planejadorfinanceiro.presentation.bloqueio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planejadorfinanceiro.domain.repository.PinRepository
import com.example.planejadorfinanceiro.domain.repository.ResultadoValidacaoPin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BloqueioPinViewModel(
    private val pinRepository: PinRepository
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoBloqueioPin>(
        EstadoBloqueioPin.Aguardando
    )

    val estado: StateFlow<EstadoBloqueioPin> =
        _estado.asStateFlow()

    fun validarPin(pin: String) {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return

        viewModelScope.launch {
            try {
                _estado.value = when (
                    val resultado = pinRepository.validarPin(pin)
                ) {
                    ResultadoValidacaoPin.Valido ->
                        EstadoBloqueioPin.Desbloqueado

                    ResultadoValidacaoPin.Incorreto ->
                        EstadoBloqueioPin.PinIncorreto

                    is ResultadoValidacaoPin.Bloqueado ->
                        EstadoBloqueioPin.TemporariamenteBloqueado(
                            resultado.tempoRestanteMillis
                        )
                }
            } catch (erro: Exception) {
                _estado.value = EstadoBloqueioPin.Erro
            }
        }
    }

    fun limparEstado() {
        _estado.value = EstadoBloqueioPin.Aguardando
    }
}
