package com.example.planejadorfinanceiro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContaDao {

    @Query("SELECT * FROM contas ORDER BY vencimentoMillis ASC")
    fun observarTodas(): Flow<List<ContaEntity>>

    @Query("SELECT * FROM contas WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): ContaEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(conta: ContaEntity): Long

    @Update
    suspend fun atualizar(conta: ContaEntity)

    @Query("UPDATE contas SET situacao = 'PAGA' WHERE id = :id")
    suspend fun marcarComoPaga(id: Long)

    @Query("DELETE FROM contas WHERE id = :id")
    suspend fun excluir(id: Long)
}
