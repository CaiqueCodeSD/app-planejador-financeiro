package com.example.planejadorfinanceiro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ContaDao {

    @Query("SELECT * FROM contas ORDER BY vencimentoMillis ASC")
    fun observarTodas(): Flow<List<ContaEntity>>

    @Query("SELECT * FROM contas WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): ContaEntity?

    @Query("""
        SELECT * FROM contas
        WHERE statusSincronizacao = 'PENDENTE'
        ORDER BY atualizadaEmMillis ASC
    """)
    suspend fun buscarPendentesSincronizacao(): List<ContaEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(conta: ContaEntity): Long

    @Update
    suspend fun atualizar(conta: ContaEntity)

    @Query("""
        UPDATE contas
        SET situacao = 'PAGA',
            atualizadaEmMillis = :atualizadaEmMillis,
            statusSincronizacao = 'PENDENTE'
        WHERE id = :id
    """)
    suspend fun marcarComoPaga(
        id: Long,
        atualizadaEmMillis: Long
    )

    @Query("""
        UPDATE contas
        SET statusSincronizacao = 'SINCRONIZADA'
        WHERE uuid = :uuid
          AND atualizadaEmMillis = :atualizadaEmMillis
          AND statusSincronizacao = 'PENDENTE'
    """)
    suspend fun marcarComoSincronizada(
        uuid: String,
        atualizadaEmMillis: Long
    ): Int

    @Query("DELETE FROM contas WHERE id = :id")
    suspend fun excluir(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registrarExclusaoPendente(
        exclusao: ExclusaoPendenteEntity
    )

    @Query("""
    SELECT * FROM exclusoes_pendentes
    ORDER BY excluidaEmMillis ASC
""")
    suspend fun buscarExclusoesPendentes(): List<ExclusaoPendenteEntity>

    @Query("DELETE FROM exclusoes_pendentes WHERE uuid = :uuid")
    suspend fun removerExclusaoPendente(uuid: String)

    @Transaction
    suspend fun excluirComRegistroPendente(id: Long, excluidaEmMillis: Long) {
        val conta = buscarPorId(id) ?: return

        registrarExclusaoPendente(
            ExclusaoPendenteEntity(
                uuid = conta.uuid,
                excluidaEmMillis = excluidaEmMillis
            )
        )

        excluir(id)
    }

}
