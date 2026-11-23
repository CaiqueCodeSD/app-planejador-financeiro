package com.example.planejadorfinanceiro.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidPinMaterialProtector : PinMaterialProtector {

    companion object {
        private const val ALIAS_CHAVE = "planejador_financeiro_pin_key"
        private const val PROVIDER_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMACAO = "AES/GCM/NoPadding"

        private const val TAMANHO_CHAVE = 256
        private const val TAMANHO_IV = 12
        private const val TAMANHO_TAG = 128
    }

    private val lock = Any()

    override fun proteger(dados: ByteArray): MaterialPinProtegido {
        require(dados.isNotEmpty()) {
            "Não é possível proteger dados vazios."
        }

        synchronized(lock) {
            val cipher = Cipher.getInstance(TRANSFORMACAO)

            cipher.init(
                Cipher.ENCRYPT_MODE,
                obterOuCriarChave()
            )

            return MaterialPinProtegido(
                iv = cipher.iv,
                conteudo = cipher.doFinal(dados)
            )
        }
    }

    override fun desproteger(
        material: MaterialPinProtegido
    ): ByteArray {
        require(material.iv.size == TAMANHO_IV) {
            "IV inválido."
        }

        require(material.conteudo.size >= TAMANHO_TAG / 8) {
            "Conteúdo protegido inválido."
        }

        synchronized(lock) {
            val cipher = Cipher.getInstance(TRANSFORMACAO)

            cipher.init(
                Cipher.DECRYPT_MODE,
                obterChaveExistente(),
                GCMParameterSpec(TAMANHO_TAG, material.iv)
            )

            return cipher.doFinal(material.conteudo)
        }
    }

    private fun obterOuCriarChave(): SecretKey {
        val keyStore = obterKeyStore()

        val chaveExistente = keyStore.getKey(
            ALIAS_CHAVE,
            null
        ) as? SecretKey

        if (chaveExistente != null) {
            return chaveExistente
        }

        val gerador = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            PROVIDER_KEYSTORE
        )

        gerador.init(
            KeyGenParameterSpec.Builder(
                ALIAS_CHAVE,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setKeySize(TAMANHO_CHAVE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )

        return gerador.generateKey()
    }

    private fun obterChaveExistente(): SecretKey {
        val chave = obterKeyStore().getKey(
            ALIAS_CHAVE,
            null
        ) as? SecretKey

        return checkNotNull(chave) {
            "A chave de proteção do PIN não foi encontrada."
        }
    }

    private fun obterKeyStore(): KeyStore =
        KeyStore.getInstance(PROVIDER_KEYSTORE).apply {
            load(null)
        }
}