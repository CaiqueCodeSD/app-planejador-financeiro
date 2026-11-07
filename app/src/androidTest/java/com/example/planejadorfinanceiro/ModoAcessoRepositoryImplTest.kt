package com.example.planejadorfinanceiro

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.planejadorfinanceiro.data.repository.ModoAcessoRepositoryImpl
import com.example.planejadorfinanceiro.domain.model.ModoAcesso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class ModoAcessoRepositoryImplTest {

    private lateinit var repository: ModoAcessoRepositoryImpl
    private lateinit var scope: CoroutineScope
    private lateinit var arquivoTeste: File

    @Before
    fun configurar() {
        arquivoTeste = File.createTempFile(
            "modo_acesso",
            ".preferences_pb"
        )
        arquivoTeste.delete()

        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { arquivoTeste }
        )

        repository = ModoAcessoRepositoryImpl(dataStore)
    }

    @After
    fun finalizar() {
        scope.cancel()
        arquivoTeste.delete()
    }

    @Test
    fun modoInicialDeveSerNaoConfigurado() = runBlocking {
        val modo = repository.observarModoAcesso().first()

        assertEquals(
            ModoAcesso.NAO_CONFIGURADO,
            modo
        )
    }

    @Test
    fun deveSalvarModoConectadoGoogle() = runBlocking {
        repository.salvarModoAcesso(ModoAcesso.CONECTADO_GOOGLE)

        val modo = repository.observarModoAcesso().first()

        assertEquals(ModoAcesso.CONECTADO_GOOGLE, modo)
    }

    @Test
    fun deveAtualizarModoParaVisitante() = runBlocking {
        repository.salvarModoAcesso(ModoAcesso.CONECTADO_GOOGLE)
        repository.salvarModoAcesso(ModoAcesso.VISITANTE)

        val modo = repository.observarModoAcesso().first()

        assertEquals(ModoAcesso.VISITANTE, modo)
    }
}