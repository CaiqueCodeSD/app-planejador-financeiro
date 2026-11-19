package com.example.planejadorfinanceiro

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.planejadorfinanceiro.data.security.AndroidPinMaterialProtector
import com.example.planejadorfinanceiro.data.security.MaterialPinProtegido
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidPinMaterialProtectorTest {

    private val protector = AndroidPinMaterialProtector()

    @Test
    fun deveProtegerEDesprotegerDadosCorretamente() {
        val dadosOriginais = "material criptografico do PIN"
            .toByteArray()

        val material = protector.proteger(dadosOriginais)
        val dadosRecuperados = protector.desproteger(material)

        assertArrayEquals(dadosOriginais, dadosRecuperados)
    }

    @Test
    fun deveGerarIVsDiferentesParaCadaOperacao() {
        val dados = "material do PIN".toByteArray()

        val primeiro = protector.proteger(dados)
        val segundo = protector.proteger(dados)

        assertNotEquals(
            primeiro.iv.toList(),
            segundo.iv.toList()
        )
    }

    @Test
    fun naoDeveAceitarConteudoAdulterado() {
        val dados = "material do PIN".toByteArray()
        val material = protector.proteger(dados)

        val conteudoAdulterado = material.conteudo.copyOf().apply {
            this[0] = (this[0].toInt() xor 1).toByte()
        }

        val materialAdulterado = MaterialPinProtegido(
            iv = material.iv,
            conteudo = conteudoAdulterado
        )

        assertThrows(Exception::class.java) {
            protector.desproteger(materialAdulterado)
        }
    }

    @Test
    fun naoDeveAceitarIVComTamanhoInvalido() {
        val material = MaterialPinProtegido(
            iv = ByteArray(8),
            conteudo = ByteArray(32)
        )

        assertThrows(IllegalArgumentException::class.java) {
            protector.desproteger(material)
        }
    }
}