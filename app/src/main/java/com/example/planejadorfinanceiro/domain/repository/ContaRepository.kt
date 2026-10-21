package com.example.planejadorfinanceiro.domain.repository

import com.example.planejadorfinanceiro.domain.model.Conta
import kotlinx.coroutines.flow.Flow

interface ContaRepository {

    fun observarContas(): Flow<List<Conta>>

    suspend fun buscarPorId(id: Long): Conta?

    suspend fun salvar(conta: Conta): Long

    suspend fun marcarComoPaga(id: Long)

    suspend fun excluir(id: Long)
}
