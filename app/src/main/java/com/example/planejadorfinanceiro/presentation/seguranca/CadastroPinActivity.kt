package com.example.planejadorfinanceiro.presentation.seguranca

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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

class CadastroPinActivity : AppCompatActivity() {

    private val viewModel: CadastroPinViewModel by viewModel()

    private val gerenciadorBloqueioPin: GerenciadorBloqueioPin by inject()

    private lateinit var etNovoPin: EditText
    private lateinit var etConfirmacaoPin: EditText
    private lateinit var tvErroCadastroPin: TextView
    private lateinit var btnAtivarProtecao: Button
    private lateinit var btnContinuarSemPin: Button

    private lateinit var btnMostrarNovoPin: ImageButton
    private lateinit var btnMostrarConfirmacaoPin: ImageButton

    private var novoPinVisivel = false
    private var confirmacaoPinVisivel = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_pin)

        etNovoPin = findViewById(R.id.etNovoPin)
        etConfirmacaoPin = findViewById(R.id.etConfirmacaoPin)
        tvErroCadastroPin = findViewById(R.id.tvErroCadastroPin)
        btnAtivarProtecao = findViewById(R.id.btnAtivarProtecao)
        btnContinuarSemPin = findViewById(R.id.btnContinuarSemPin)

        btnMostrarNovoPin = findViewById(R.id.btnMostrarNovoPin)
        btnMostrarConfirmacaoPin = findViewById(R.id.btnMostrarConfirmacaoPin)

        configurarBotoes()
        observarEstado()
    }

    private fun configurarBotoes() {

        btnAtivarProtecao.setOnClickListener {
            tvErroCadastroPin.visibility = View.GONE
            btnAtivarProtecao.isEnabled = false

            viewModel.limparEstado()

            viewModel.cadastrarPin(
                pin = etNovoPin.text.toString(),
                confirmacao = etConfirmacaoPin.text.toString()
            )
        }

        btnContinuarSemPin.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        btnMostrarNovoPin.setOnClickListener {
            novoPinVisivel = !novoPinVisivel

            alternarVisibilidade(
                campo = etNovoPin,
                botao = btnMostrarNovoPin,
                visivel = novoPinVisivel
            )
        }

        btnMostrarConfirmacaoPin.setOnClickListener {
            confirmacaoPinVisivel = !confirmacaoPinVisivel

            alternarVisibilidade(
                campo = etConfirmacaoPin,
                botao = btnMostrarConfirmacaoPin,
                visivel = confirmacaoPinVisivel
            )
        }
    }

    private fun alternarVisibilidade(
        campo: EditText,
        botao: ImageButton,
        visivel: Boolean
    ) {
        val posicaoInicio = campo.selectionStart
        val posicaoFim = campo.selectionEnd

        campo.transformationMethod = if (visivel) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }

        val tamanhoTexto = campo.text.length

        if (posicaoInicio >= 0 && posicaoFim >= 0) {
            campo.setSelection(
                posicaoInicio.coerceAtMost(tamanhoTexto),
                posicaoFim.coerceAtMost(tamanhoTexto)
            )
        }

        if (visivel) {
            botao.setImageResource(R.drawable.ic_visibility_on)
            botao.contentDescription = "Ocultar PIN"
        } else {
            botao.setImageResource(R.drawable.ic_visibility_off)
            botao.contentDescription = "Mostrar PIN"
        }
    }

    private fun observarEstado() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estado.collect { estado ->
                    when (estado) {
                        EstadoCadastroPin.Inicial -> {
                            btnAtivarProtecao.isEnabled = true
                        }

                        EstadoCadastroPin.Cadastrado -> {
                            gerenciadorBloqueioPin.desbloquear()
                            setResult(RESULT_OK)
                            finish()
                        }

                        EstadoCadastroPin.PinInvalido -> {
                            exibirErro(
                                "Informe um PIN válido de quatro dígitos."
                            )
                        }

                        EstadoCadastroPin.PinsDiferentes -> {
                            etConfirmacaoPin.text.clear()
                            exibirErro(
                                "Os PINs informados não coincidem."
                            )
                        }

                        EstadoCadastroPin.Erro -> {
                            exibirErro(
                                "Não foi possível cadastrar o PIN. Tente novamente."
                            )
                        }
                    }
                }
            }
        }
    }

    private fun exibirErro(mensagem: String) {
        tvErroCadastroPin.text = mensagem
        tvErroCadastroPin.visibility = View.VISIBLE
        btnAtivarProtecao.isEnabled = true
    }
}
