package com.example.planejadorfinanceiro.data.remote

import com.example.planejadorfinanceiro.domain.model.Conta
import com.example.planejadorfinanceiro.domain.model.Movimentacao

object GoogleSheetsMapper {

    fun mapearConta(conta: Conta): List<Any> {
        return listOf(
            conta.uuid,
            conta.descricao,
            conta.valorCentavos,
            conta.vencimentoMillis,
            conta.situacao.name,
            conta.criadaEmMillis,
            conta.atualizadaEmMillis,
            conta.versao
        )
    }

    fun mapearMovimentacao(
        movimentacao: Movimentacao
    ): List<Any> {
        return listOf(
            movimentacao.uuid,
            movimentacao.descricao,
            movimentacao.valorCentavos,
            movimentacao.tipo.name,
            movimentacao.dataMillis,
            movimentacao.categoria,
            movimentacao.criadaEmMillis,
            movimentacao.atualizadaEmMillis,
            movimentacao.versao
        )
    }
}
