package com.example.planejadorfinanceiro.domain.repository

import com.example.planejadorfinanceiro.domain.model.Conta
import com.example.planejadorfinanceiro.domain.model.Movimentacao
import com.example.planejadorfinanceiro.domain.model.ExclusaoPendente

interface SincronizacaoDataSource {

    suspend fun sincronizarConta(conta: Conta)

    suspend fun sincronizarMovimentacao(
        movimentacao: Movimentacao
    )

    suspend fun sincronizarExclusao(
        exclusao: ExclusaoPendente
    )
}