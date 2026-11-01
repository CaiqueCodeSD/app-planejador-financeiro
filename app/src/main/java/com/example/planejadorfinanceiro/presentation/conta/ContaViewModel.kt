package com.example.planejadorfinanceiro.presentation.conta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planejadorfinanceiro.domain.model.Conta
import com.example.planejadorfinanceiro.domain.repository.ContaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CadastroContaUiState(
    val salvando: Boolean = false,
    val mensagemErro: String? = null,
    val cadastroConcluido: Boolean = false
)

data class ExclusaoContaUiState(
    val excluindo: Boolean = false,
    val mensagemSucesso: String? = null,
    val mensagemErro: String? = null
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

    private val _exclusaoUiState = MutableStateFlow(ExclusaoContaUiState())
    val exclusaoUiState = _exclusaoUiState.asStateFlow()

    fun salvarConta(
        descricao: String,
        valorEmReais: String,
        vencimentoMillis: Long,
        contaId: Long? = null
    ) {
        val descricaoValidada = descricao.trim()
        val valor = valorEmReais.trim().replace(",", ".").toBigDecimalOrNull()

        if (descricaoValidada.isBlank()) {
            atualizarErro("Informe a descrição da conta.")
            return
        }

        if (valor == null || valor <= java.math.BigDecimal.ZERO) {
            atualizarErro("Informe um valor válido maior que zero.")
            return
        }

        val centavos = try {
            valor.movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) {
            atualizarErro("O valor deve ter no máximo duas casas decimais.")
            return
        }

        viewModelScope.launch {
            _uiState.value = CadastroContaUiState(salvando = true)

            try {
                val conta = if (contaId == null) {
                    Conta(
                        descricao = descricaoValidada,
                        valorCentavos = centavos,
                        vencimentoMillis = vencimentoMillis
                    )
                } else {
                    val existente = repository.buscarPorId(contaId)
                        ?: throw IllegalStateException("A conta não foi encontrada.")

                    existente.copy(
                        descricao = descricaoValidada,
                        valorCentavos = centavos,
                        vencimentoMillis = vencimentoMillis
                    )
                }

                repository.salvar(conta)
                _uiState.value = CadastroContaUiState(cadastroConcluido = true)

            } catch (_: Exception) {
                _uiState.value = CadastroContaUiState(
                    mensagemErro = if (contaId == null) {
                        "Não foi possível salvar a conta."
                    } else {
                        "Não foi possível atualizar a conta."
                    }
                )
            }
        }
    }

    fun marcarComoPaga(id: Long) {
        viewModelScope.launch {
            try {
                repository.marcarComoPaga(id)
            } catch (_: Exception) {
                // O tratamento visual será aprimorado em etapa própria.
            }
        }
    }

    fun excluirConta(id: Long) {
        viewModelScope.launch {
            _exclusaoUiState.value = ExclusaoContaUiState(excluindo = true)

            try {
                repository.excluir(id)

                _exclusaoUiState.value = ExclusaoContaUiState(
                    mensagemSucesso = "Conta excluída com sucesso!"
                )
            } catch (_: Exception) {
                _exclusaoUiState.value = ExclusaoContaUiState(
                    mensagemErro = "Não foi possível excluir a conta."
                )
            }
        }
    }

    fun limparFeedbackExclusao() {
        _exclusaoUiState.value = ExclusaoContaUiState()
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
