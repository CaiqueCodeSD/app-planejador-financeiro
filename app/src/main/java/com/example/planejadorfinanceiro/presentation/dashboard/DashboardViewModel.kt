package com.example.planejadorfinanceiro.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planejadorfinanceiro.domain.model.Movimentacao
import com.example.planejadorfinanceiro.domain.model.TipoMovimentacao
import com.example.planejadorfinanceiro.domain.repository.MovimentacaoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class DashboardUiState(
    val saldoCentavos: Long = 0L,
    val receitasCentavos: Long = 0L,
    val despesasCentavos: Long = 0L,
    val movimentacoesRecentes: List<Movimentacao> = emptyList()
)

class DashboardViewModel(
    repository: MovimentacaoRepository
) : ViewModel() {

    private val inicioDoMesMillis: Long
    private val inicioDoProximoMesMillis: Long

    init {
        val inicioDoMes = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        inicioDoMesMillis = inicioDoMes.timeInMillis

        inicioDoProximoMesMillis = (inicioDoMes.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }.timeInMillis
    }

    val uiState = repository.observarMovimentacoes()
        .map { movimentacoes ->
            val movimentacoesDoMes = movimentacoes.filter {
                it.dataMillis >= inicioDoMesMillis &&
                        it.dataMillis < inicioDoProximoMesMillis
            }

            val receitas = movimentacoesDoMes
                .filter { it.tipo == TipoMovimentacao.RECEITA }
                .sumOf { it.valorCentavos }

            val despesas = movimentacoesDoMes
                .filter { it.tipo == TipoMovimentacao.DESPESA }
                .sumOf { it.valorCentavos }

            DashboardUiState(
                saldoCentavos = receitas - despesas,
                receitasCentavos = receitas,
                despesasCentavos = despesas,
                movimentacoesRecentes = movimentacoesDoMes.take(3)
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState()
        )
}
