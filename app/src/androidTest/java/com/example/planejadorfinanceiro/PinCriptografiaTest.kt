package com.example.planejadorfinanceiro.data.security

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PinCriptografiaTest {

    private lateinit var criptografia: PinCriptografia

    @Before
    fun configurar() {
        criptografia = PinCriptografia()
    }

    @Test
    fun deveGerarSaltsDiferentes() {
        val salt1 = criptografia.gerarSalt()
        val salt2 = criptografia.gerarSalt()

        assertEquals(16, salt1.size)
        assertFalse(salt1.contentEquals(salt2))
    }

    @Test
    fun deveGerarMesmoResultadoParaMesmoPinESalt() {
        val salt = criptografia.gerarSalt()

        val resultado1 = criptografia.derivarPin("1234", salt)
        val resultado2 = criptografia.derivarPin("1234", salt)

        assertArrayEquals(resultado1, resultado2)
    }

    @Test
    fun deveGerarResultadosDiferentesParaPinsDiferentes() {
        val salt = criptografia.gerarSalt()

        val resultado1 = criptografia.derivarPin("1234", salt)
        val resultado2 = criptografia.derivarPin("5678", salt)

        assertFalse(resultado1.contentEquals(resultado2))
    }

    @Test
    fun deveAceitarPinCorreto() {
        val salt = criptografia.gerarSalt()
        val armazenado = criptografia.derivarPin("1234", salt)
        val informado = criptografia.derivarPin("1234", salt)

        assertTrue(criptografia.comparar(armazenado, informado))
    }

    @Test
    fun deveRejeitarPinIncorreto() {
        val salt = criptografia.gerarSalt()
        val armazenado = criptografia.derivarPin("1234", salt)
        val informado = criptografia.derivarPin("5678", salt)

        assertFalse(criptografia.comparar(armazenado, informado))
    }

    @Test(expected = IllegalArgumentException::class)
    fun naoDeveAceitarPinComTresDigitos() {
        criptografia.derivarPin("123", criptografia.gerarSalt())
    }

    @Test(expected = IllegalArgumentException::class)
    fun naoDeveAceitarPinComLetras() {
        criptografia.derivarPin("12a4", criptografia.gerarSalt())
    }
}