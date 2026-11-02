package com.example.planejadorfinanceiro.domain.repository

import com.example.planejadorfinanceiro.domain.model.ExclusaoPendente

interface ExclusaoPendenteRepository {
    suspend fun buscarTodas(): List<ExclusaoPendente>

    suspend fun confirmarSincronizacao(
        uuid: String,
        excluidaEmMillis: Long
    ): Boolean
}