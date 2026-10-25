package com.example.planejadorfinanceiro.di

import com.example.planejadorfinanceiro.data.local.FinanceiroDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        FinanceiroDatabase.getInstance(androidContext())
    }

    single {
        get<FinanceiroDatabase>().contaDao()
    }

    single {
        get<FinanceiroDatabase>().movimentacaoDao()
    }
}
