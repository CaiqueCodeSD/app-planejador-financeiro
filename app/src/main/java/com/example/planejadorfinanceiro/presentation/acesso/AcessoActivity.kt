package com.example.planejadorfinanceiro.presentation.acesso

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.planejadorfinanceiro.MainActivity
import com.example.planejadorfinanceiro.R
import com.example.planejadorfinanceiro.domain.model.ModoAcesso
import com.example.planejadorfinanceiro.domain.repository.GoogleAuthorizationRepository
import com.example.planejadorfinanceiro.domain.repository.ResultadoAutorizacao
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.example.planejadorfinanceiro.domain.usecase.ConectarGoogleUseCase

class AcessoActivity : AppCompatActivity() {

    private val conectarGoogleUseCase: ConectarGoogleUseCase by inject()

    private val viewModel: ModoAcessoViewModel by viewModel()

    private val googleAuthorizationRepository:
            GoogleAuthorizationRepository by inject()

    private var navegacaoRealizada = false

    private val autorizacaoGoogleLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { resultado ->

            if (resultado.resultCode == RESULT_OK) {
                try {
                    val autorizacao = Identity
                        .getAuthorizationClient(this)
                        .getAuthorizationResultFromIntent(resultado.data)

                    processarTokenGoogle(autorizacao.accessToken)

                } catch (erro: Exception) {
                    exibirMensagem(
                        "Falha ao concluir autorização Google."
                    )
                }
            } else {
                exibirMensagem("Autorização Google cancelada.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acesso)

        configurarBotoes()
        observarModoAcesso()
    }

    private fun configurarBotoes() {
        findViewById<Button>(R.id.btnContinuarVisitante)
            .setOnClickListener {
                viewModel.salvarModoAcesso(ModoAcesso.VISITANTE)
            }

        findViewById<Button>(R.id.btnConectarGoogle)
            .setOnClickListener {
                iniciarAutorizacaoGoogle()
            }
    }

    private fun iniciarAutorizacaoGoogle() {
        lifecycleScope.launch {
            try {
                when (
                    val resultado =
                        googleAuthorizationRepository.obterAccessToken()
                ) {
                    is ResultadoAutorizacao.TokenDisponivel -> {
                        processarTokenGoogle(resultado.token)
                    }

                    ResultadoAutorizacao.RequerInteracao -> {
                        solicitarConsentimentoGoogle()
                    }
                }
            } catch (erro: Exception) {
                exibirMensagem(
                    "Não foi possível obter autorização Google."
                )
            }
        }
    }

    private fun solicitarConsentimentoGoogle() {
        val requisicao = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope(
                        "https://www.googleapis.com/auth/drive.file"
                    )
                )
            )
            .build()

        Identity.getAuthorizationClient(this)
            .authorize(requisicao)
            .addOnSuccessListener { resultado ->

                if (resultado.hasResolution()) {
                    val pendingIntent = resultado.pendingIntent

                    if (pendingIntent != null) {
                        autorizacaoGoogleLauncher.launch(
                            IntentSenderRequest.Builder(
                                pendingIntent.intentSender
                            ).build()
                        )
                    } else {
                        exibirMensagem(
                            "Não foi possível iniciar a autorização Google."
                        )
                    }
                } else {
                    processarTokenGoogle(resultado.accessToken)
                }
            }
            .addOnFailureListener {
                exibirMensagem(
                    "Não foi possível solicitar autorização Google."
                )
            }
    }

    private fun processarTokenGoogle(accessToken: String?) {
        if (accessToken.isNullOrBlank()) {
            exibirMensagem("O Google não forneceu um token de acesso.")
            return
        }

        lifecycleScope.launch {
            try {
                conectarGoogleUseCase(accessToken)

                exibirMensagem(
                    "Google conectado e planilha configurada com sucesso!"
                )

            } catch (erro: Exception) {
                exibirMensagem(
                    "Não foi possível concluir a conexão com o Google."
                )
            }
        }
    }

    private fun observarModoAcesso() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.modoAcesso.collect { modo ->

                    when (modo) {
                        ModoAcesso.NAO_CONFIGURADO -> Unit

                        ModoAcesso.VISITANTE,
                        ModoAcesso.CONECTADO_GOOGLE -> {
                            abrirAplicativo()
                        }
                    }
                }
            }
        }
    }

    private fun abrirAplicativo() {
        if (navegacaoRealizada) return

        navegacaoRealizada = true

        startActivity(
            Intent(this, MainActivity::class.java)
        )

        finish()
    }

    private fun exibirMensagem(mensagem: String) {
        Toast.makeText(
            this,
            mensagem,
            Toast.LENGTH_LONG
        ).show()
    }
}
