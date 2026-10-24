
package com.example.planejadorfinanceiro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.planejadorfinanceiro.domain.model.StatusSincronizacao
import java.util.UUID

@Entity(tableName = "contas")
data class ContaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val descricao: String,
    val valorCentavos: Long,
    val vencimentoMillis: Long,
    val situacao: String = "PENDENTE",
    val criadaEmMillis: Long = System.currentTimeMillis(),
    val atualizadaEmMillis: Long = System.currentTimeMillis(),
    val statusSincronizacao: String = StatusSincronizacao.PENDENTE.name
)
