package com.example.planejadorfinanceiro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimentacaoDao {

    @Query("""
        SELECT * FROM movimentacoes
        ORDER BY dataMillis DESC, id DESC
    """)
    fun observarTodas(): Flow<List<MovimentacaoEntity>>

    @Query("""
        SELECT * FROM movimentacoes
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun buscarPorId(id: Long): MovimentacaoEntity?

    @Query("""
        SELECT * FROM movimentacoes
        WHERE statusSincronizacao = 'PENDENTE'
        ORDER BY atualizadaEmMillis ASC
    """)
    suspend fun buscarPendentesSincronizacao(): List<MovimentacaoEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(movimentacao: MovimentacaoEntity): Long

    @Update
    suspend fun atualizar(movimentacao: MovimentacaoEntity)

    @Query("""
        UPDATE movimentacoes
        SET statusSincronizacao = 'SINCRONIZADA'
        WHERE uuid = :uuid
          AND atualizadaEmMillis = :atualizadaEmMillis
          AND statusSincronizacao = 'PENDENTE'
    """)
    suspend fun marcarComoSincronizada(
        uuid: String,
        atualizadaEmMillis: Long
    ): Int

    @Query("""
        SELECT COALESCE(SUM(valorCentavos), 0)
        FROM movimentacoes
        WHERE tipo = :tipo
          AND dataMillis >= :inicioMillis
          AND dataMillis < :fimMillis
    """)
    fun observarTotalPorTipoNoPeriodo(
        tipo: String,
        inicioMillis: Long,
        fimMillis: Long
    ): Flow<Long>
}
