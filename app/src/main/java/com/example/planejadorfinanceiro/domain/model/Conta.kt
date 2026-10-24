package com.example.planejadorfinanceiro.domain.model

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
    val criadaEmMillis: Long = System.currentTimeMillis()
)
