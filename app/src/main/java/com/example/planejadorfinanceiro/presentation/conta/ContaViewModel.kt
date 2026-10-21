package com.example.planejadorfinanceiro.presentation.conta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planejadorfinanceiro.domain.model.Conta
import com.example.planejadorfinanceiro.domain.repository.ContaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

data class CadastroContaUiState(
    val salvando: Boolean = false,
    val mensagemErro: String? = null,
    val cadastroConcluido: Boolean = false
)

class ContaViewModel(
    private val repository: ContaRepository
) : ViewModel() {

    val contas = repository.observarContas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(CadastroContaUiState())
    val uiState = _uiState.asStateFlow()

    fun salvarConta(
        descricao: String,
        valorEmReais: String,
        vencimentoMillis: Long
    ) {
        val descricaoValidada = descricao.trim()
        val valor = valorEmReais.trim()
            .replace(",", ".")
            .toBigDecimalOrNull()

        if (descricaoValidada.isBlank()) {
            atualizarErro("Informe a descrição da conta.")
            return
        }

        if (valor == null || valor <= java.math.BigDecimal.ZERO) {
            atualizarErro("Informe um valor válido maior que zero.")
            return
        }

        val centavos = try {
            valor.movePointRight(2)
                .longValueExact()
        } catch (_: ArithmeticException) {
            atualizarErro("O valor deve ter no máximo duas casas decimais.")
            return
        }

        viewModelScope.launch {
            _uiState.value = CadastroContaUiState(salvando = true)

            try {
                repository.salvar(
                    Conta(
                        descricao = descricaoValidada,
                        valorCentavos = centavos,
                        vencimentoMillis = vencimentoMillis
                    )
                )

                _uiState.value = CadastroContaUiState(
                    cadastroConcluido = true
                )
            } catch (_: Exception) {
                _uiState.value = CadastroContaUiState(
                    mensagemErro = "Não foi possível salvar a conta."
                )
            }
        }
    }

    fun limparFeedback() {
        _uiState.value = CadastroContaUiState()
    }

    private fun atualizarErro(mensagem: String) {
        _uiState.value = CadastroContaUiState(
            mensagemErro = mensagem
        )
    }
}
