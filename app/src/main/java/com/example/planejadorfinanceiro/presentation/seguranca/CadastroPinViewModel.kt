package com.example.planejadorfinanceiro.presentation.seguranca

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planejadorfinanceiro.domain.repository.PinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EstadoCadastroPin {
    data object Inicial : EstadoCadastroPin
    data object Cadastrado : EstadoCadastroPin
    data object PinInvalido : EstadoCadastroPin
    data object PinsDiferentes : EstadoCadastroPin
    data object Erro : EstadoCadastroPin
}

class CadastroPinViewModel(
    private val pinRepository: PinRepository
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoCadastroPin>(
        EstadoCadastroPin.Inicial
    )

    val estado: StateFlow<EstadoCadastroPin> =
        _estado.asStateFlow()

    fun cadastrarPin(pin: String, confirmacao: String) {
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            _estado.value = EstadoCadastroPin.PinInvalido
            return
        }

        if (confirmacao.length != 4 || !confirmacao.all { it.isDigit() }) {
            _estado.value = EstadoCadastroPin.PinInvalido
            return
        }

        if (pin != confirmacao) {
            _estado.value = EstadoCadastroPin.PinsDiferentes
            return
        }

        viewModelScope.launch {
            try {
                pinRepository.cadastrarPin(pin)
                _estado.value = EstadoCadastroPin.Cadastrado
            } catch (erro: Exception) {
                _estado.value = EstadoCadastroPin.Erro
            }
        }
    }

    fun limparEstado() {
        _estado.value = EstadoCadastroPin.Inicial
    }
}
