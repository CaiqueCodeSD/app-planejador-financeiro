package com.example.planejadorfinanceiro.data.repository

import android.content.Context
import com.example.planejadorfinanceiro.domain.repository.GoogleAuthorizationRepository
import com.example.planejadorfinanceiro.domain.repository.ResultadoAutorizacao
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GoogleAuthorizationRepositoryImpl(
    context: Context
) : GoogleAuthorizationRepository {

    private val authorizationClient =
        Identity.getAuthorizationClient(context.applicationContext)

    private val escopos = listOf(
        Scope("https://www.googleapis.com/auth/drive.file")
    )

    override suspend fun obterAccessToken(): ResultadoAutorizacao {
        val requisicao = AuthorizationRequest.builder()
            .setRequestedScopes(escopos)
            .build()

        val resultado = authorizationClient
            .authorize(requisicao)
            .aguardar()

        if (resultado.hasResolution()) {
            return ResultadoAutorizacao.RequerInteracao
        }

        val token = resultado.accessToken

        if (token.isNullOrBlank()) {
            throw IllegalStateException(
                "O Google não forneceu um token de acesso."
            )
        }

        return ResultadoAutorizacao.TokenDisponivel(token)
    }

    override suspend fun revogarAcesso() {
        val requisicao = RevokeAccessRequest.builder()
            .setScopes(escopos)
            .build()

        authorizationClient
            .revokeAccess(requisicao)
            .aguardar()
    }

    private suspend fun <T> Task<T>.aguardar(): T =
        suspendCancellableCoroutine { continuacao ->

            addOnSuccessListener { resultado ->
                if (continuacao.isActive) {
                    continuacao.resume(resultado)
                }
            }

            addOnFailureListener { erro ->
                if (continuacao.isActive) {
                    continuacao.resumeWithException(erro)
                }
            }
        }
}
