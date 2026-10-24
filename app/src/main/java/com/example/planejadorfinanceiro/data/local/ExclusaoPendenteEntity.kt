package com.example.planejadorfinanceiro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exclusoes_pendentes")
data class ExclusaoPendenteEntity(
    @PrimaryKey
    val uuid: String,
    val excluidaEmMillis: Long
)
