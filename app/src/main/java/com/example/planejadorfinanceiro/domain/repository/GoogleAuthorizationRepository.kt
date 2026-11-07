package com.example.planejadorfinanceiro.domain.repository

interface GoogleAuthorizationRepository {
    suspend fun revogarAcesso()
}
