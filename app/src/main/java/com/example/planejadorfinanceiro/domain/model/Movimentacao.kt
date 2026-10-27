package com.example.planejadorfinanceiro.domain.model

import java.util.UUID

data class Movimentacao(
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val descricao: String,
    val valorCentavos: Long,
    val tipo: TipoMovimentacao,
    val dataMillis: Long,
    val categoria: String,
    val criadaEmMillis: Long = System.currentTimeMillis(),
    val atualizadaEmMillis: Long = System.currentTimeMillis(),
    val statusSincronizacao: StatusSincronizacao =
        StatusSincronizacao.PENDENTE
)