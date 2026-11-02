package com.example.planejadorfinanceiro

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.planejadorfinanceiro.data.local.FinanceiroDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanceiroDatabaseMigrationTest {

    private val databaseName = "migration-test.db"

    @Test
    fun migration4To5_preservesPendingDeletionsAndCreatesUniqueIndex() {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        context.deleteDatabase(databaseName)

        // Cria um banco temporário com a estrutura da versão 4.
        val helper = object : SQLiteOpenHelper(
            context,
            databaseName,
            null,
            4
        ) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE contas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        descricao TEXT NOT NULL,
                        valorCentavos INTEGER NOT NULL,
                        vencimentoMillis INTEGER NOT NULL,
                        situacao TEXT NOT NULL,
                        criadaEmMillis INTEGER NOT NULL,
                        uuid TEXT NOT NULL DEFAULT '',
                        atualizadaEmMillis INTEGER NOT NULL DEFAULT 0,
                        statusSincronizacao TEXT NOT NULL DEFAULT 'PENDENTE'
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE exclusoes_pendentes (
                        uuid TEXT NOT NULL PRIMARY KEY,
                        excluidaEmMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE movimentacoes (
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

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX index_movimentacoes_uuid
                    ON movimentacoes(uuid)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO contas (
                        descricao,
                        valorCentavos,
                        vencimentoMillis,
                        situacao,
                        criadaEmMillis,
                        uuid
                    ) VALUES (
                        'Conta de teste',
                        10000,
                        123456789,
                        'PENDENTE',
                        123456789,
                        'uuid-conta'
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO exclusoes_pendentes (
                        uuid,
                        excluidaEmMillis
                    ) VALUES ('uuid-legado', 123456789)
                    """.trimIndent()
                )
            }

            override fun onUpgrade(
                db: SQLiteDatabase,
                oldVersion: Int,
                newVersion: Int
            ) = Unit
        }

        helper.writableDatabase
        helper.close()

        // O Room executa a migração real 4 -> 5.
        val database = Room.databaseBuilder(
            context,
            FinanceiroDatabase::class.java,
            databaseName
        )
            .addMigrations(FinanceiroDatabase.MIGRATION_4_5)
            .build()

        try {
            val db = database.openHelper.writableDatabase

            db.query(
                """
                SELECT uuid, excluidaEmMillis, tipoRegistro
                FROM exclusoes_pendentes
                WHERE uuid = 'uuid-legado'
                """
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("uuid-legado", cursor.getString(0))
                assertEquals(123456789L, cursor.getLong(1))
                assertEquals("LEGADO", cursor.getString(2))
            }

            db.query("PRAGMA index_list('contas')").use { cursor ->
                var indexFound = false

                while (cursor.moveToNext()) {
                    val name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name")
                    )

                    if (name == "index_contas_uuid") {
                        assertEquals(
                            1,
                            cursor.getInt(
                                cursor.getColumnIndexOrThrow("unique")
                            )
                        )
                        indexFound = true
                    }
                }

                assertTrue(
                    "Índice único de UUID das contas não encontrado",
                    indexFound
                )
            }
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
