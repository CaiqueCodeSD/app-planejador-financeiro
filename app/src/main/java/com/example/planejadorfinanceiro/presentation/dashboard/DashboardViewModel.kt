package com.example.planejadorfinanceiro.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planejadorfinanceiro.domain.model.Movimentacao
import com.example.planejadorfinanceiro.domain.model.TipoMovimentacao
import com.example.planejadorfinanceiro.domain.repository.MovimentacaoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DashboardUiState(
    val saldoCentavos: Long = 0L,
    val receitasCentavos: Long = 0L,
    val despesasCentavos: Long = 0L,
    val movimentacoesRecentes: List<Movimentacao> = emptyList(),
    val mesSelecionado: Int = Calendar.getInstance().get(Calendar.MONTH),
    val anoSelecionado: Int = Calendar.getInstance().get(Calendar.YEAR),
    val tituloPeriodo: String = ""
)

class DashboardViewModel(
    private val repository: MovimentacaoRepository
) : ViewModel() {

    private val periodoSelecionado = kotlinx.coroutines.flow.MutableStateFlow(
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    )

    fun mesAnterior() {
        periodoSelecionado.value = (periodoSelecionado.value.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
    }

    fun proximoMes() {
        periodoSelecionado.value = (periodoSelecionado.value.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
    }

    val uiState = combine(
        repository.observarMovimentacoes(),
        periodoSelecionado
    ) { movimentacoes, periodo ->

        val inicioDoMes = (periodo.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val inicioDoProximoMes = (inicioDoMes.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }

        val inicioMillis = inicioDoMes.timeInMillis
        val fimMillis = inicioDoProximoMes.timeInMillis

        val movimentacoesDoMes = movimentacoes.filter {
            it.dataMillis >= inicioMillis && it.dataMillis < fimMillis
        }

        val receitas = movimentacoesDoMes
            .filter { it.tipo == TipoMovimentacao.RECEITA }
            .sumOf { it.valorCentavos }

        val despesas = movimentacoesDoMes
            .filter { it.tipo == TipoMovimentacao.DESPESA }
            .sumOf { it.valorCentavos }

        val tituloPeriodo = SimpleDateFormat(
            "MMMM yyyy",
            Locale("pt", "BR")
        ).format(inicioDoMes.time)
            .replaceFirstChar { it.uppercase(Locale("pt", "BR")) }

        DashboardUiState(
            saldoCentavos = receitas - despesas,
            receitasCentavos = receitas,
            despesasCentavos = despesas,
            movimentacoesRecentes = movimentacoesDoMes.take(3),
            mesSelecionado = inicioDoMes.get(Calendar.MONTH),
            anoSelecionado = inicioDoMes.get(Calendar.YEAR),
            tituloPeriodo = tituloPeriodo
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(
            tituloPeriodo = SimpleDateFormat(
                "MMMM yyyy",
                Locale("pt", "BR")
            ).format(Calendar.getInstance().time)
                .replaceFirstChar { it.uppercase(Locale("pt", "BR")) }
        )
    )
}
