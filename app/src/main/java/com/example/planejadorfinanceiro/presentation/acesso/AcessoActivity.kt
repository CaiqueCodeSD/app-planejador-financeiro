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
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class AcessoActivity : AppCompatActivity() {

    private val viewModel: ModoAcessoViewModel by viewModel()

    private var navegacaoRealizada = false

    private val autorizacaoGoogleLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { resultado ->

            if (resultado.resultCode == RESULT_OK) {
                try {
                    val autorizacao = Identity
                        .getAuthorizationClient(this)
                        .getAuthorizationResultFromIntent(
                            resultado.data
                        )

                    processarResultadoAutorizacao(
                        autorizacao.accessToken
                    )

                } catch (erro: Exception) {
                    Toast.makeText(
                        this,
                        "Falha ao concluir autorização Google.",
                        Toast.LENGTH_LONG
                    ).show()
                }
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
                viewModel.salvarModoAcesso(
                    ModoAcesso.VISITANTE
                )
            }

        findViewById<Button>(R.id.btnConectarGoogle)
            .setOnClickListener {
                solicitarAutorizacaoGoogle()
            }
    }

    private fun solicitarAutorizacaoGoogle() {
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
                        Toast.makeText(
                            this,
                            "Não foi possível iniciar a autorização Google.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {
                    processarResultadoAutorizacao(
                        resultado.accessToken
                    )
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Não foi possível solicitar autorização Google.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun processarResultadoAutorizacao(
        accessToken: String?
    ) {
        if (accessToken.isNullOrBlank()) {
            Toast.makeText(
                this,
                "O Google não forneceu um token de acesso.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        Toast.makeText(
            this,
            "Autorização Google recebida com sucesso!",
            Toast.LENGTH_LONG
        ).show()
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
}
