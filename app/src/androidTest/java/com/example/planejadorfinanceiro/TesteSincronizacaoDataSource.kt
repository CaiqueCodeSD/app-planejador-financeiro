package com.example.planejadorfinanceiro

import com.example.planejadorfinanceiro.domain.model.Conta
import com.example.planejadorfinanceiro.domain.model.Movimentacao
import com.example.planejadorfinanceiro.domain.model.ExclusaoPendente
import com.example.planejadorfinanceiro.domain.repository.SincronizacaoDataSource

enum class OperacaoSincronizacao {
    EXCLUSAO,
    CONTA,
    MOVIMENTACAO
}

class TesteSincronizacaoDataSource : SincronizacaoDataSource {

    val chamadas = mutableListOf<String>()

    var falharEm: OperacaoSincronizacao? = null

    private fun registrarOperacao(
        operacao: OperacaoSincronizacao,
        identificador: String
    ) {
        chamadas.add("${operacao.name}:$identificador")

        if (falharEm == operacao) {
            throw Exception("Falha simulada em ${operacao.name}")
        }
    }

    override suspend fun sincronizarConta(conta: Conta) {
        registrarOperacao(
            OperacaoSincronizacao.CONTA,
            conta.uuid
        )
    }

    override suspend fun sincronizarMovimentacao(
        movimentacao: Movimentacao
    ) {
        registrarOperacao(
            OperacaoSincronizacao.MOVIMENTACAO,
            movimentacao.uuid
        )
    }

    override suspend fun sincronizarExclusao(
        exclusao: ExclusaoPendente
    ) {
        registrarOperacao(
            OperacaoSincronizacao.EXCLUSAO,
            exclusao.uuid
        )
    }
}
