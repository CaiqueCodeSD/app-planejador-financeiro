package com.example.planejadorfinanceiro.domain.repository

import com.example.planejadorfinanceiro.domain.model.ModoAcesso
import kotlinx.coroutines.flow.Flow

interface ModoAcessoRepository {

    fun observarModoAcesso(): Flow<ModoAcesso>

    suspend fun salvarModoAcesso(modo: ModoAcesso)
}