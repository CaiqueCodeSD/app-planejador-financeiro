package com.example.planejadorfinanceiro.domain.repository

interface GoogleAuthorizationRepository {
    suspend fun obterAccessToken(): ResultadoAutorizacao
    suspend fun revogarAcesso()
}

sealed interface ResultadoAutorizacao {
    data class TokenDisponivel(val token: String) : ResultadoAutorizacao
    data object RequerInteracao : ResultadoAutorizacao
}