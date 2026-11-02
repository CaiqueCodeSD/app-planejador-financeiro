package com.example.planejadorfinanceiro

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.example.planejadorfinanceiro.data.local.ExclusaoPendenteEntity
import com.example.planejadorfinanceiro.data.local.FinanceiroDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExclusaoPendenteDaoTest {

    private lateinit var database: FinanceiroDatabase

    @Before
    fun configurarBanco() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            FinanceiroDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun fecharBanco() {
        database.close()
    }

    @Test
    fun registrarEConsultarExclusaoPendente() = runBlocking {
        val exclusao = ExclusaoPendenteEntity(
            uuid = "conta-teste-001",
            excluidaEmMillis = 1000L,
            tipoRegistro = "CONTA"
        )

        database.exclusaoPendenteDao().registrar(exclusao)

        val resultado = database.exclusaoPendenteDao().buscarTodas()

        assertEquals(1, resultado.size)
        assertEquals("conta-teste-001", resultado.first().uuid)
        assertEquals("CONTA", resultado.first().tipoRegistro)
    }

    @Test
    fun removerExclusaoPendente() = runBlocking {
        val exclusao = ExclusaoPendenteEntity(
            uuid = "movimentacao-teste-001",
            excluidaEmMillis = 2000L,
            tipoRegistro = "MOVIMENTACAO"
        )

        val dao = database.exclusaoPendenteDao()

        dao.registrar(exclusao)

        val linhasRemovidas = dao.remover(
            uuid = exclusao.uuid,
            excluidaEmMillis = exclusao.excluidaEmMillis
        )

        val resultado = dao.buscarTodas()

        assertEquals(1, linhasRemovidas)
        assertEquals(0, resultado.size)
    }

    @Test
    fun consultarFilaDeveRespeitarOrdemCronologica() = runBlocking {
        val dao = database.exclusaoPendenteDao()

        dao.registrar(
            ExclusaoPendenteEntity(
                uuid = "registro-mais-recente",
                excluidaEmMillis = 3000L,
                tipoRegistro = "CONTA"
            )
        )

        dao.registrar(
            ExclusaoPendenteEntity(
                uuid = "registro-mais-antigo",
                excluidaEmMillis = 1000L,
                tipoRegistro = "MOVIMENTACAO"
            )
        )

        val resultado = dao.buscarTodas()

        assertEquals(2, resultado.size)
        assertEquals("registro-mais-antigo", resultado[0].uuid)
        assertEquals("registro-mais-recente", resultado[1].uuid)
    }

    @Test
    fun removerExclusaoPendente_comTimestampDiferente_devePreservarRegistro() = runBlocking {
        val exclusao = ExclusaoPendenteEntity(
            uuid = "movimentacao-teste-002",
            excluidaEmMillis = 3000L,
            tipoRegistro = "MOVIMENTACAO"
        )

        val dao = database.exclusaoPendenteDao()

        dao.registrar(exclusao)

        val linhasRemovidas = dao.remover(
            uuid = exclusao.uuid,
            excluidaEmMillis = 4000L
        )

        val resultado = dao.buscarTodas()

        assertEquals(0, linhasRemovidas)
        assertEquals(1, resultado.size)
    }

}
