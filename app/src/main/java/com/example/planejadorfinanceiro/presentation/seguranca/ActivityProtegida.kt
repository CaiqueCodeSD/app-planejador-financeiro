package com.example.planejadorfinanceiro.presentation.seguranca

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.planejadorfinanceiro.data.security.GerenciadorBloqueioPin
import com.example.planejadorfinanceiro.presentation.bloqueio.BloqueioPinActivity
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

abstract class ActivityProtegida : AppCompatActivity() {

    companion object {
        private const val REQUEST_BLOQUEIO_PIN = 1001
    }

    private val gerenciadorBloqueioPin: GerenciadorBloqueioPin by inject()

    private var telaBloqueioAberta = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                gerenciadorBloqueioPin.estado.collect { estado ->

                    if (!estado.inicializado) return@collect

                    if (!estado.pinAtivado) {
                        telaBloqueioAberta = false
                        return@collect
                    }

                    if (!estado.desbloqueado && !telaBloqueioAberta) {
                        telaBloqueioAberta = true

                        val intent = Intent(
                            this@ActivityProtegida,
                            BloqueioPinActivity::class.java
                        )

                        startActivityForResult(
                            intent,
                            REQUEST_BLOQUEIO_PIN
                        )
                    }
                }
            }
        }
    }

    @Deprecated("Utilizado para receber o resultado do desbloqueio.")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_BLOQUEIO_PIN) {
            if (resultCode != RESULT_OK) {
                finish()
            } else {
                telaBloqueioAberta = false
            }
        }
    }
}
