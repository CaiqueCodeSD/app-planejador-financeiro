package com.example.planejadorfinanceiro.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinCriptografia {

    companion object {
        private const val TAMANHO_SALT = 16
        private const val ITERACOES = 600_000
        private const val TAMANHO_CHAVE = 256
    }

    private val secureRandom = SecureRandom()

    fun gerarSalt(): ByteArray {
        return ByteArray(TAMANHO_SALT).also {
            secureRandom.nextBytes(it)
        }
    }

    fun derivarPin(
        pin: String,
        salt: ByteArray
    ): ByteArray {
        require(pin.matches(Regex("^\\d{4}$"))) {
            "O PIN deve conter exatamente quatro dígitos."
        }

        val spec = PBEKeySpec(
            pin.toCharArray(),
            salt,
            ITERACOES,
            TAMANHO_CHAVE
        )

        return try {
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun comparar(
        chaveArmazenada: ByteArray,
        chaveInformada: ByteArray
    ): Boolean {
        return MessageDigest.isEqual(
            chaveArmazenada,
            chaveInformada
        )
    }
}