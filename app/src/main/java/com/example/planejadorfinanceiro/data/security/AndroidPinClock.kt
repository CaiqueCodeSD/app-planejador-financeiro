package com.example.planejadorfinanceiro.data.security

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

class AndroidPinClock(
    private val context: Context
) : PinClock {

    override fun obterHorarioAtual(): Long =
        System.currentTimeMillis()

    override fun obterTempoDesdeInicializacao(): Long =
        SystemClock.elapsedRealtime()

    override fun obterContagemInicializacoes(): Int =
        Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.BOOT_COUNT,
            -1
        )
}
