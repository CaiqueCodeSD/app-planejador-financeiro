package com.example.planejadorfinanceiro.data.security

interface PinClock {
    fun obterHorarioAtual(): Long
    fun obterTempoDesdeInicializacao(): Long
    fun obterContagemInicializacoes(): Int
}
