package com.example.planejadorfinanceiro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contas")
data class ContaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val descricao: String,
    val valorCentavos: Long,
    val vencimentoMillis: Long,
    val situacao: String = "PENDENTE",
    val criadaEmMillis: Long = System.currentTimeMillis()
)
