package com.example.planejadorfinanceiro.domain.model

sealed interface EstadoSincronizacao {

    data object Ociosa : EstadoSincronizacao

    data object EmAndamento : EstadoSincronizacao

    data class Concluida(
        val totalSincronizado: Int
    ) : EstadoSincronizacao

    data class Erro(
        val mensagem: String
    ) : EstadoSincronizacao
}