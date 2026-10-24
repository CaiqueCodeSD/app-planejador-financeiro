package com.example.planejadorfinanceiro.domain.model

import java.util.UUID

enum class SituacaoConta {
    PENDENTE,
    PAGA
}

data class Conta(
    val id: Long = 0,
    val descricao: String,
    val valorCentavos: Long,
    val vencimentoMillis: Long,
    val situacao: SituacaoConta = SituacaoConta.PENDENTE,
    val criadaEmMillis: Long = System.currentTimeMillis(),
    val uuid: String = java.util.UUID.randomUUID().toString(),
    val atualizadaEmMillis: Long = System.currentTimeMillis(),
    val statusSincronizacao: StatusSincronizacao = StatusSincronizacao.PENDENTE
)
