package com.example.planejadorfinanceiro.presentation.sincronizacao

sealed interface SincronizacaoUiState {

    data object Ociosa : SincronizacaoUiState

    data object EmAndamento : SincronizacaoUiState

    data class Concluida(
        val totalSincronizado: Int
    ) : SincronizacaoUiState

    data class Erro(
        val mensagem: String
    ) : SincronizacaoUiState

    data object AutorizacaoNecessaria : SincronizacaoUiState
}
