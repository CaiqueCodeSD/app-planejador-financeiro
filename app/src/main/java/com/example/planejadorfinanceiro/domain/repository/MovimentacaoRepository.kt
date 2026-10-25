package com.example.planejadorfinanceiro.domain.repository

import com.example.planejadorfinanceiro.domain.model.Movimentacao
import com.example.planejadorfinanceiro.domain.model.TipoMovimentacao
import kotlinx.coroutines.flow.Flow

interface MovimentacaoRepository {

    fun observarMovimentacoes(): Flow<List<Movimentacao>>

    fun observarTotalPorTipoNoPeriodo(
        tipo: TipoMovimentacao,
        inicioMillis: Long,
        fimMillis: Long
    ): Flow<Long>

    suspend fun buscarPorId(id: Long): Movimentacao?

    suspend fun salvar(movimentacao: Movimentacao): Long

    suspend fun buscarPendentesSincronizacao(): List<Movimentacao>

    suspend fun marcarComoSincronizada(
        uuid: String,
        atualizadaEmMillis: Long
    )
}
