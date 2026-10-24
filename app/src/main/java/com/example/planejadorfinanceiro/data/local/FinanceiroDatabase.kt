package com.example.planejadorfinanceiro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ContaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FinanceiroDatabase : RoomDatabase() {

    abstract fun contaDao(): ContaDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceiroDatabase? = null

        fun getInstance(context: Context): FinanceiroDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinanceiroDatabase::class.java,
                    "planejador_financeiro.db"
                ).build().also { database ->
                    INSTANCE = database
                }
            }
        }
    }
}
