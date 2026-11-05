package com.example.planejadorfinanceiro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ContaDao {

    @Query("SELECT * FROM contas ORDER BY vencimentoMillis ASC")
    fun observarTodas(): Flow<List<ContaEntity>>

    @Query("SELECT * FROM contas WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): ContaEntity?

    @Query("SELECT * FROM contas WHERE uuid = :uuid LIMIT 1")
    suspend fun buscarPorUuid(uuid: String): ContaEntity?

    @Query("""
        SELECT * FROM contas
        WHERE statusSincronizacao = 'PENDENTE'
        ORDER BY atualizadaEmMillis ASC
    """)
    suspend fun buscarPendentesSincronizacao(): List<ContaEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(conta: ContaEntity): Long

    @Query("""
        UPDATE contas
        SET descricao = :descricao,
            valorCentavos = :valorCentavos,
            vencimentoMillis = :vencimentoMillis,
            situacao = :situacao,
            atualizadaEmMillis = :atualizadaEmMillis,
            statusSincronizacao = 'PENDENTE',
            versao = versao + 1
        WHERE id = :id
    """)
    suspend fun atualizar(
        id: Long,
        descricao: String,
        valorCentavos: Long,
        vencimentoMillis: Long,
        situacao: String,
        atualizadaEmMillis: Long
    ): Int

    @Query("""
        UPDATE contas
        SET situacao = 'PAGA',
            atualizadaEmMillis = :atualizadaEmMillis,
            statusSincronizacao = 'PENDENTE',
            versao = versao + 1
        WHERE id = :id
    """)
    suspend fun marcarComoPaga(
        id: Long,
        atualizadaEmMillis: Long
    ): Int

    @Query("""
        UPDATE contas
        SET statusSincronizacao = 'SINCRONIZADA'
        WHERE uuid = :uuid
          AND versao = :versao
          AND statusSincronizacao = 'PENDENTE'
    """)
    suspend fun marcarComoSincronizada(
        uuid: String,
        versao: Long
    ): Int

    @Query("DELETE FROM contas WHERE id = :id")
    suspend fun excluir(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registrarExclusaoPendente(
        exclusao: ExclusaoPendenteEntity
    )

    @Transaction
    suspend fun excluirComRegistroPendente(
        id: Long,
        excluidaEmMillis: Long
    ) {
        val conta = buscarPorId(id) ?: return

        registrarExclusaoPendente(
            ExclusaoPendenteEntity(
                uuid = conta.uuid,
                excluidaEmMillis = excluidaEmMillis,
                tipoRegistro = "CONTA"
            )
        )

        excluir(id)
    }
}
