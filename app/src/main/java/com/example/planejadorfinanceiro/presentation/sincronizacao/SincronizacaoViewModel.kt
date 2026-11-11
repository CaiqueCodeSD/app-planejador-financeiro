package com.example.planejadorfinanceiro.presentation.sincronizacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planejadorfinanceiro.domain.model.EstadoSincronizacao
import com.example.planejadorfinanceiro.domain.repository.SincronizacaoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SincronizacaoViewModel(
    private val repository: SincronizacaoRepository
) : ViewModel() {

    val uiState: StateFlow<SincronizacaoUiState> =
        repository.estado
            .map(::mapearEstado)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SincronizacaoUiState.Ociosa
            )

    fun sincronizar() {
        viewModelScope.launch {
            repository.sincronizar()
        }
    }

    private fun mapearEstado(
        estado: EstadoSincronizacao
    ): SincronizacaoUiState {
        return when (estado) {
            EstadoSincronizacao.Ociosa ->
                SincronizacaoUiState.Ociosa

            EstadoSincronizacao.EmAndamento ->
                SincronizacaoUiState.EmAndamento

            is EstadoSincronizacao.Concluida ->
                SincronizacaoUiState.Concluida(
                    totalSincronizado = estado.totalSincronizado
                )

            is EstadoSincronizacao.Erro ->
                SincronizacaoUiState.Erro(
                    mensagem = estado.mensagem
                )

            EstadoSincronizacao.AutorizacaoNecessaria ->
                SincronizacaoUiState.AutorizacaoNecessaria
        }
    }
}
