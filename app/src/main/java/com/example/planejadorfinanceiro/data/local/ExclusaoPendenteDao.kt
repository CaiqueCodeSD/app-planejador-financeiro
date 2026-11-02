package com.example.planejadorfinanceiro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExclusaoPendenteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registrar(exclusao: ExclusaoPendenteEntity)

    @Query("""
        SELECT * FROM exclusoes_pendentes
        ORDER BY excluidaEmMillis ASC
    """)
    suspend fun buscarTodas(): List<ExclusaoPendenteEntity>

    @Query("""
    DELETE FROM exclusoes_pendentes
    WHERE uuid = :uuid
    AND excluidaEmMillis = :excluidaEmMillis
""")
    suspend fun remover(
        uuid: String,
        excluidaEmMillis: Long
    ): Int
}
