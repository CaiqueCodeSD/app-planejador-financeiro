package com.example.planejadorfinanceiro.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.planejadorfinanceiro.data.local.modoAcessoDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataStoreModule = module {

    single<DataStore<Preferences>> {
        androidContext().modoAcessoDataStore
    }

}