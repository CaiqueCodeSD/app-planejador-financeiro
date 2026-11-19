package com.example.planejadorfinanceiro.presentation.bloqueio

sealed interface EstadoBloqueioPin {
    data object Aguardando : EstadoBloqueioPin

    data object Desbloqueado : EstadoBloqueioPin

    data object PinIncorreto : EstadoBloqueioPin

    data class TemporariamenteBloqueado(
        val tempoRestanteMillis: Long
    ) : EstadoBloqueioPin

    data object Erro : EstadoBloqueioPin
}