
package com.example.planejadorfinanceiro.presentation.movimentacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planejadorfinanceiro.domain.model.Movimentacao
import com.example.planejadorfinanceiro.domain.model.TipoMovimentacao
import com.example.planejadorfinanceiro.domain.repository.MovimentacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

data class MovimentacaoUiState(
    val salvando: Boolean = false,
    val excluindo: Boolean = false,
    val mensagemErro: String? = null,
    val mensagemSucesso: String? = null,
    val operacaoConcluida: Boolean = false
)

class MovimentacaoViewModel(
    private val repository: MovimentacaoRepository
) : ViewModel() {

    val movimentacoes = repository.observarMovimentacoes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(MovimentacaoUiState())
    val uiState = _uiState.asStateFlow()

    suspend fun buscarMovimentacaoPorId(id: Long): Movimentacao? {
        return repository.buscarPorId(id)
    }

    fun salvarMovimentacao(
        descricao: String,
        valorEmReais: String,
        tipo: TipoMovimentacao,
        dataMillis: Long,
        categoria: String,
        movimentacaoId: Long? = null
    ) {
        val descricaoValidada = descricao.trim()
        val categoriaValidada = categoria.trim()
        val valor = converterValor(valorEmReais)

        if (descricaoValidada.isBlank()) {
            atualizarErro("Informe a descrição da movimentação.")
            return
        }

        if (categoriaValidada.isBlank()) {
            atualizarErro("Informe a categoria da movimentação.")
            return
        }

        if (valor == null || valor <= BigDecimal.ZERO) {
            atualizarErro("Informe um valor válido maior que zero.")
            return
        }

        val centavos = try {
            valor.setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact()
        } catch (_: ArithmeticException) {
            atualizarErro("O valor deve ter no máximo duas casas decimais.")
            return
        }

        viewModelScope.launch {
            _uiState.value = MovimentacaoUiState(salvando = true)

            try {
                val movimentacao = if (movimentacaoId == null) {
                    Movimentacao(
                        descricao = descricaoValidada,
                        valorCentavos = centavos,
                        tipo = tipo,
                        dataMillis = dataMillis,
                        categoria = categoriaValidada
                    )
                } else {
                    val existente = repository.buscarPorId(movimentacaoId)
                        ?: throw IllegalStateException(
                            "A movimentação não foi encontrada."
                        )

                    existente.copy(
                        descricao = descricaoValidada,
                        valorCentavos = centavos,
                        tipo = tipo,
                        dataMillis = dataMillis,
                        categoria = categoriaValidada
                    )
                }

                repository.salvar(movimentacao)

                _uiState.value = MovimentacaoUiState(
                    mensagemSucesso = if (movimentacaoId == null) {
                        "Movimentação cadastrada com sucesso."
                    } else {
                        "Movimentação atualizada com sucesso."
                    },
                    operacaoConcluida = true
                )
            } catch (_: Exception) {
                _uiState.value = MovimentacaoUiState(
                    mensagemErro = if (movimentacaoId == null) {
                        "Não foi possível cadastrar a movimentação."
                    } else {
                        "Não foi possível atualizar a movimentação."
                    }
                )
            }
        }
    }

    fun excluirMovimentacao(id: Long) {
        viewModelScope.launch {
            _uiState.value = MovimentacaoUiState(excluindo = true)

            try {
                repository.excluir(id)

                _uiState.value = MovimentacaoUiState(
                    mensagemSucesso = "Movimentação excluída com sucesso.",
                    operacaoConcluida = true
                )
            } catch (_: Exception) {
                _uiState.value = MovimentacaoUiState(
                    mensagemErro = "Não foi possível excluir a movimentação."
                )
            }
        }
    }

    fun limparFeedback() {
        _uiState.value = MovimentacaoUiState()
    }

    private fun converterValor(valorEmReais: String): BigDecimal? {
        val valorNormalizado = valorEmReais.trim()

        if (valorNormalizado.isBlank()) return null

        val normalizado = when {
            ',' in valorNormalizado && '.' in valorNormalizado -> {
                if (valorNormalizado.lastIndexOf(',') >
                    valorNormalizado.lastIndexOf('.')
                ) {
                    valorNormalizado.replace(".", "").replace(",", ".")
                } else {
                    valorNormalizado.replace(",", "")
                }
            }

            ',' in valorNormalizado -> valorNormalizado.replace(",", ".")

            else -> valorNormalizado
        }

        return normalizado.toBigDecimalOrNull()
    }

    private fun atualizarErro(mensagem: String) {
        _uiState.value = MovimentacaoUiState(
            mensagemErro = mensagem
        )
    }
}
