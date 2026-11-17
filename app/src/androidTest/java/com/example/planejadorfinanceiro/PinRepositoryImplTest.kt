package com.example.planejadorfinanceiro

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.planejadorfinanceiro.data.repository.PinRepositoryImpl
import com.example.planejadorfinanceiro.data.security.PinCriptografia
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

    private var horarioAtual = 1_000_000L

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

        repository = PinRepositoryImpl(
            dataStore = dataStore,
            criptografia = PinCriptografia(),
            obterHorarioAtual = { horarioAtual }
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

        horarioAtual += 15_000L

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

        horarioAtual += 60_000L

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
            obterHorarioAtual = { horarioAtual }
        )

        assertEquals(
            ResultadoValidacaoPin.Bloqueado(60_000L),
            novoRepositorio.validarPin("1234")
        )
    }
}
