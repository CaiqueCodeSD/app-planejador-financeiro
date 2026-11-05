package com.example.planejadorfinanceiro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ContaEntity::class,
        ExclusaoPendenteEntity::class,
        MovimentacaoEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class FinanceiroDatabase : RoomDatabase() {

    abstract fun movimentacaoDao(): MovimentacaoDao
    abstract fun contaDao(): ContaDao
    abstract fun exclusaoPendenteDao(): ExclusaoPendenteDao

    companion object {

        @Volatile
        private var INSTANCE: FinanceiroDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE contas ADD COLUMN uuid TEXT NOT NULL DEFAULT ''"
                )

                database.execSQL(
                    "ALTER TABLE contas ADD COLUMN atualizadaEmMillis INTEGER NOT NULL DEFAULT 0"
                )

                database.execSQL(
                    """
                    ALTER TABLE contas
                    ADD COLUMN statusSincronizacao TEXT NOT NULL DEFAULT 'PENDENTE'
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    UPDATE contas
                    SET uuid = lower(
                        hex(randomblob(4)) || '-' ||
                        hex(randomblob(2)) || '-4' ||
                        substr(hex(randomblob(2)), 2) || '-' ||
                        substr('89ab', abs(random()) % 4 + 1, 1) ||
                        substr(hex(randomblob(2)), 2) || '-' ||
                        hex(randomblob(6))
                    ),
                    atualizadaEmMillis = criadaEmMillis,
                    statusSincronizacao = 'PENDENTE'
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS exclusoes_pendentes (
                        uuid TEXT NOT NULL PRIMARY KEY,
                        excluidaEmMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS movimentacoes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uuid TEXT NOT NULL,
                        descricao TEXT NOT NULL,
                        valorCentavos INTEGER NOT NULL,
                        tipo TEXT NOT NULL,
                        dataMillis INTEGER NOT NULL,
                        categoria TEXT NOT NULL,
                        criadaEmMillis INTEGER NOT NULL,
                        atualizadaEmMillis INTEGER NOT NULL,
                        statusSincronizacao TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    index_movimentacoes_uuid
                    ON movimentacoes(uuid)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL(
                    """
            ALTER TABLE exclusoes_pendentes
            ADD COLUMN tipoRegistro TEXT NOT NULL DEFAULT 'LEGADO'
            """.trimIndent()
                )

                database.execSQL(
                    """
            CREATE UNIQUE INDEX IF NOT EXISTS
            index_contas_uuid
            ON contas(uuid)
            """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            ALTER TABLE contas
            ADD COLUMN versao INTEGER NOT NULL DEFAULT 1
            """.trimIndent()
                )

                database.execSQL(
                    """
            ALTER TABLE movimentacoes
            ADD COLUMN versao INTEGER NOT NULL DEFAULT 1
            """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): FinanceiroDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinanceiroDatabase::class.java,
                    "planejador_financeiro.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5
                    )
                    .build()
                    .also { database ->
                        INSTANCE = database
                    }
            }
        }
    }
}