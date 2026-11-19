package com.example.planejadorfinanceiro.presentation.bloqueio

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.planejadorfinanceiro.R
import com.example.planejadorfinanceiro.data.security.GerenciadorBloqueioPin
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class BloqueioPinActivity : AppCompatActivity() {

    private val viewModel: BloqueioPinViewModel by viewModel()

    private val gerenciadorBloqueioPin: GerenciadorBloqueioPin by inject()

    private lateinit var etPin: EditText
    private lateinit var tvErroPin: TextView
    private lateinit var btnDesbloquear: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bloqueio_pin)

        etPin = findViewById(R.id.etPin)
        tvErroPin = findViewById(R.id.tvErroPin)
        btnDesbloquear = findViewById(R.id.btnDesbloquear)

        configurarBotao()
        observarEstado()
    }

    private fun configurarBotao() {
        btnDesbloquear.setOnClickListener {
            val pin = etPin.text.toString()

            if (pin.length != 4 || !pin.all { it.isDigit() }) {
                exibirErro("Informe os quatro dígitos do PIN.")
                return@setOnClickListener
            }

            btnDesbloquear.isEnabled = false
            tvErroPin.visibility = View.GONE

            viewModel.validarPin(pin)
        }
    }

    private fun observarEstado() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estado.collect { estado ->
                    when (estado) {
                        EstadoBloqueioPin.Aguardando -> {
                            btnDesbloquear.isEnabled = true
                        }

                        EstadoBloqueioPin.Desbloqueado -> {
                            gerenciadorBloqueioPin.desbloquear()
                            setResult(RESULT_OK)
                            finish()
                        }

                        EstadoBloqueioPin.PinIncorreto -> {
                            etPin.text.clear()
                            exibirErro("PIN incorreto. Tente novamente.")
                        }

                        is EstadoBloqueioPin.TemporariamenteBloqueado -> {
                            val segundos = (
                                    estado.tempoRestanteMillis + 999
                                    ) / 1000

                            exibirErro(
                                "Muitas tentativas. Tente novamente em $segundos segundos."
                            )
                        }

                        EstadoBloqueioPin.Erro -> {
                            exibirErro(
                                "Não foi possível validar o PIN. Tente novamente."
                            )
                        }
                    }
                }
            }
        }
    }

    private fun exibirErro(mensagem: String) {
        tvErroPin.text = mensagem
        tvErroPin.visibility = View.VISIBLE
        btnDesbloquear.isEnabled = true
    }
}
