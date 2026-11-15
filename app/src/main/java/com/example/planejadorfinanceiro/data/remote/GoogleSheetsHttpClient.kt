package com.example.planejadorfinanceiro.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class GoogleSheetsHttpClient(
    private val client: OkHttpClient
) {

    suspend fun executar(
        url: String,
        accessToken: String,
        metodo: String = "GET",
        json: String? = null
    ): String = withContext(Dispatchers.IO) {

        val requestBody = json?.toRequestBody(
            "application/json; charset=utf-8".toMediaType()
        )

        val request = Request.Builder()
            .url(url)
            .header(
                "Authorization",
                "Bearer $accessToken"
            )
            .header("Accept", "application/json")
            .method(metodo, requestBody)
            .build()

        client.newCall(request).execute().use { response ->

            val resposta = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IOException(
                    "Erro na API Google: HTTP ${response.code}"
                )
            }

            resposta
        }
    }
}
