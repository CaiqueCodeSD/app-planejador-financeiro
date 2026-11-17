package com.example.planejadorfinanceiro.domain.model

data class EstadoBloqueioPin(
    val pinAtivado: Boolean = false,
    val desbloqueado: Boolean = false,
    val inicializado: Boolean = false
)