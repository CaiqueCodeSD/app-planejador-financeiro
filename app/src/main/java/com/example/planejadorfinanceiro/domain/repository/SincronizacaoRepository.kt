package com.example.planejadorfinanceiro.domain.repository

import com.example.planejadorfinanceiro.domain.model.EstadoSincronizacao
import kotlinx.coroutines.flow.StateFlow

interface SincronizacaoRepository {

    val estado: StateFlow<EstadoSincronizacao>

    suspend fun sincronizar()
}