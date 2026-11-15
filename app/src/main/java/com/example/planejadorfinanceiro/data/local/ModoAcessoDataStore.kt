package com.example.planejadorfinanceiro.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.modoAcessoDataStore by preferencesDataStore(
    name = "modo_acesso"
)