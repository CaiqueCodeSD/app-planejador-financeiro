package com.example.planejadorfinanceiro

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.planejadorfinanceiro.data.repository.PinRepositoryImpl
import com.example.planejadorfinanceiro.data.security.MaterialPinProtegido
import com.example.planejadorfinanceiro.data.security.PinClock
import com.example.planejadorfinanceiro.data.security.PinCriptografia
import com.example.planejadorfinanceiro.data.security.PinMaterialProtector
import com.example.planejadorfinanceiro.domain.repository.ResultadoValidacaoPin
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PinRepositoryImplTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: PinRepositoryImpl
    private lateinit var arquivoTemporario: File
    private lateinit var scope: CoroutineScope
    private lateinit var pinClock: FakePinClock

    private val materialProtector = FakePinMaterialProtector()

    private class FakePinClock(
        var horarioAtual: Long = 1_000_000L,
        var tempoDesdeInicializacao: Long = 500_000L,
        var contagemInicializacoes: Int = 1
    ) : PinClock {

        override fun obterHorarioAtual(): Long = horarioAtual

        override fun obterTempoDesdeInicializacao(): Long =
            tempoDesdeInicializacao

        override fun obterContagemInicializacoes(): Int =
            contagemInicializacoes
    }

    private class FakePinMaterialProtectorComFalha :
        PinMaterialProtector {

        override fun proteger(dados: ByteArray): MaterialPinProtegido {
            return MaterialPinProtegido(
                iv = ByteArray(12),
                conteudo = dados.copyOf()
            )
        }

        override fun desproteger(
            material: MaterialPinProtegido
        ): ByteArray {
            throw SecurityException(
                "Falha na integridade do material protegido."
            )
        }
    }

    @Before
    fun configurar() {
        arquivoTemporario = File.createTempFile(
            "pin-test",
            ".preferences_pb"
        )

        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { arquivoTemporario }
        )

        pinClock = FakePinClock()

        repository = PinRepositoryImpl(
            dataStore = dataStore,
            criptografia = PinCriptografia(),
            materialProtector = materialProtector,
            pinClock = pinClock
        )
    }

    @After
    fun limpar() {
        scope.cancel()
        arquivoTemporario.delete()
    }

    @Test
    fun pinDeveIniciarDesativado() = runBlocking {
        assertFalse(repository.observarPinAtivado().first())
    }

    @Test
    fun deveCadastrarEValidarPinCorretamente() = runBlocking {
        repository.cadastrarPin("1234")

        assertTrue(repository.observarPinAtivado().first())

        assertEquals(
            ResultadoValidacaoPin.Valido,
            repository.validarPin("1234")
        )
    }

    @Test
    fun deveRejeitarPinIncorreto() = runBlocking {
        repository.cadastrarPin("1234")

        assertEquals(
            ResultadoValidacaoPin.Incorreto,
            repository.validarPin("9999")
        )
    }

    @Test
    fun deveAlterarPinSomenteAposValidarOPinAtual() = runBlocking {
        repository.cadastrarPin("1234")

        assertEquals(
            ResultadoValidacaoPin.Valido,
            repository.alterarPin("1234", "5678")
        )

        assertEquals(
            ResultadoValidacaoPin.Incorreto,
            repository.validarPin("1234")
        )

        assertEquals(
            ResultadoValidacaoPin.Valido,
            repository.validarPin("5678")
        )
    }

    @Test
    fun deveDesativarPinAposValidarOPinAtual() = runBlocking {
        repository.cadastrarPin("1234")

        assertEquals(
            ResultadoValidacaoPin.Valido,
            repository.desativarPin("1234")
        )

        assertFalse(repository.observarPinAtivado().first())
    }

    @Test
    fun deveBloquearAposCincoTentativasIncorretas() = runBlocking {
        repository.cadastrarPin("1234")

        repeat(4) {
            assertEquals(
                ResultadoValidacaoPin.Incorreto,
                repository.validarPin("abc")
            )
        }

        assertEquals(
            ResultadoValidacaoPin.Bloqueado(60_000L),
            repository.validarPin("abc")
        )
    }

    @Test
    fun deveInformarTempoRestanteDoBloqueio() = runBlocking {
        repository.cadastrarPin("1234")

        repeat(5) {
            repository.validarPin("abc")
        }

        pinClock.tempoDesdeInicializacao += 15_000L

        assertEquals(
            ResultadoValidacaoPin.Bloqueado(45_000L),
            repository.validarPin("1234")
        )
    }

    @Test
    fun devePermitirValidacaoAposFimDoBloqueio() = runBlocking {
        repository.cadastrarPin("1234")

        repeat(5) {
            repository.validarPin("abc")
        }

        pinClock.tempoDesdeInicializacao += 60_000L

        assertEquals(
            ResultadoValidacaoPin.Valido,
            repository.validarPin("1234")
        )
    }

    @Test
    fun devePreservarBloqueioAposRecriarRepositorio() = runBlocking {
        repository.cadastrarPin("1234")

        repeat(5) {
            repository.validarPin("abc")
        }

        val novoRepositorio = PinRepositoryImpl(
            dataStore = dataStore,
            criptografia = PinCriptografia(),
            materialProtector = materialProtector,
            pinClock = pinClock
        )

        assertEquals(
            ResultadoValidacaoPin.Bloqueado(60_000L),
            novoRepositorio.validarPin("1234")
        )
    }

    @Test
    fun devePreservarBloqueioAposReinicializacao() = runBlocking {
        repository.cadastrarPin("1234")

        repeat(5) {
            repository.validarPin("abc")
        }

        // Simula a reinicialização do aparelho.
        pinClock.contagemInicializacoes++
        pinClock.tempoDesdeInicializacao = 100L

        // Avançam 15 segundos no horário civil.
        pinClock.horarioAtual += 15_000L

        assertEquals(
            ResultadoValidacaoPin.Bloqueado(45_000L),
            repository.validarPin("1234")
        )
    }

    @Test
    fun devePropagarFalhaDeDescriptografiaSemContabilizarTentativa() =
        runBlocking {
            repository.cadastrarPin("1234")

            val repositoryComFalha = PinRepositoryImpl(
                dataStore = dataStore,
                criptografia = PinCriptografia(),
                materialProtector = FakePinMaterialProtectorComFalha(),
                pinClock = pinClock
            )

            repeat(4) {
                assertEquals(
                    ResultadoValidacaoPin.Incorreto,
                    repository.validarPin("9999")
                )
            }

            var excecaoRecebida = false

            try {
                repositoryComFalha.validarPin("1234")
            } catch (e: SecurityException) {
                excecaoRecebida = true
            }

            assertTrue(excecaoRecebida)

            assertEquals(
                ResultadoValidacaoPin.Bloqueado(60_000L),
                repository.validarPin("9999")
            )
        }

    private class FakePinMaterialProtector : PinMaterialProtector {

        private val ivTeste = ByteArray(12) { it.toByte() }
        private val chaveXor = 0x5A

        override fun proteger(
            dados: ByteArray
        ): MaterialPinProtegido {
            val conteudoProtegido = dados.map {
                (it.toInt() xor chaveXor).toByte()
            }.toByteArray()

            return MaterialPinProtegido(
                iv = ivTeste.copyOf(),
                conteudo = conteudoProtegido
            )
        }

        override fun desproteger(
            material: MaterialPinProtegido
        ): ByteArray {
            require(material.iv.contentEquals(ivTeste)) {
                "IV inválido."
            }

            return material.conteudo.map {
                (it.toInt() xor chaveXor).toByte()
            }.toByteArray()
        }
    }
}
