package com.example.planejadorfinanceiro

import android.app.Application
import com.example.planejadorfinanceiro.di.databaseModule
import com.example.planejadorfinanceiro.di.repositoryModule
import com.example.planejadorfinanceiro.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class FinanceiroApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@FinanceiroApplication)

            modules(
                databaseModule,
                repositoryModule,
                viewModelModule
            )
        }
    }
}
