package com.example.planejadorfinanceiro

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.planejadorfinanceiro.data.local.ContaEntity
import com.example.planejadorfinanceiro.data.local.FinanceiroDatabase
import com.example.planejadorfinanceiro.data.local.MovimentacaoEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExclusaoTransacionalDaoTest {

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
    fun excluirContaPendenteDeveRegistrarExclusao() = runBlocking {
        val contaDao = database.contaDao()
        val exclusaoDao = database.exclusaoPendenteDao()

        val conta = ContaEntity(
            uuid = "conta-pendente-001",
            descricao = "Conta pendente",
            valorCentavos = 15000L,
            vencimentoMillis = 1000L,
            statusSincronizacao = "PENDENTE"
        )

        val id = contaDao.inserir(conta)

        contaDao.excluirComRegistroPendente(
            id = id,
            excluidaEmMillis = 2000L
        )

        assertNull(contaDao.buscarPorId(id))

        val exclusoes = exclusaoDao.buscarTodas()

        assertEquals(1, exclusoes.size)
        assertEquals("conta-pendente-001", exclusoes.first().uuid)
        assertEquals("CONTA", exclusoes.first().tipoRegistro)
        assertEquals(2000L, exclusoes.first().excluidaEmMillis)
    }

    @Test
    fun excluirContaSincronizadaDeveRegistrarExclusao() = runBlocking {
        val contaDao = database.contaDao()
        val exclusaoDao = database.exclusaoPendenteDao()

        val conta = ContaEntity(
            uuid = "conta-sincronizada-001",
            descricao = "Conta sincronizada",
            valorCentavos = 15000L,
            vencimentoMillis = 1000L,
            statusSincronizacao = "SINCRONIZADA"
        )

        val id = contaDao.inserir(conta)

        contaDao.excluirComRegistroPendente(
            id = id,
            excluidaEmMillis = 2000L
        )

        assertNull(contaDao.buscarPorId(id))

        val exclusoes = exclusaoDao.buscarTodas()

        assertEquals(1, exclusoes.size)
        assertEquals("conta-sincronizada-001", exclusoes.first().uuid)
        assertEquals("CONTA", exclusoes.first().tipoRegistro)
        assertEquals(2000L, exclusoes.first().excluidaEmMillis)
    }

    @Test
    fun excluirMovimentacaoPendenteDeveRegistrarExclusao() = runBlocking {
        val movimentacaoDao = database.movimentacaoDao()
        val exclusaoDao = database.exclusaoPendenteDao()

        val movimentacao = MovimentacaoEntity(
            uuid = "movimentacao-pendente-001",
            descricao = "Movimentação pendente",
            valorCentavos = 25000L,
            tipo = "DESPESA",
            dataMillis = 1000L,
            categoria = "ALIMENTACAO",
            statusSincronizacao = "PENDENTE"
        )

        val id = movimentacaoDao.inserir(movimentacao)

        movimentacaoDao.excluirComRegistroPendente(
            id = id,
            excluidaEmMillis = 2000L
        )

        assertNull(movimentacaoDao.buscarPorId(id))

        val exclusoes = exclusaoDao.buscarTodas()

        assertEquals(1, exclusoes.size)
        assertEquals("movimentacao-pendente-001", exclusoes.first().uuid)
        assertEquals("MOVIMENTACAO", exclusoes.first().tipoRegistro)
        assertEquals(2000L, exclusoes.first().excluidaEmMillis)
    }

    @Test
    fun excluirMovimentacaoSincronizadaDeveRegistrarExclusao() = runBlocking {
        val movimentacaoDao = database.movimentacaoDao()
        val exclusaoDao = database.exclusaoPendenteDao()

        val movimentacao = MovimentacaoEntity(
            uuid = "movimentacao-sincronizada-001",
            descricao = "Movimentação sincronizada",
            valorCentavos = 25000L,
            tipo = "DESPESA",
            dataMillis = 1000L,
            categoria = "ALIMENTACAO",
            statusSincronizacao = "SINCRONIZADA"
        )

        val id = movimentacaoDao.inserir(movimentacao)

        movimentacaoDao.excluirComRegistroPendente(
            id = id,
            excluidaEmMillis = 2000L
        )

        assertNull(movimentacaoDao.buscarPorId(id))

        val exclusoes = exclusaoDao.buscarTodas()

        assertEquals(1, exclusoes.size)
        assertEquals(
            "movimentacao-sincronizada-001",
            exclusoes.first().uuid
        )
        assertEquals("MOVIMENTACAO", exclusoes.first().tipoRegistro)
        assertEquals(2000L, exclusoes.first().excluidaEmMillis)
    }

    @Test
    fun excluirContaInexistenteNaoDeveRegistrarExclusao() = runBlocking {
        val contaDao = database.contaDao()
        val exclusaoDao = database.exclusaoPendenteDao()

        contaDao.excluirComRegistroPendente(
            id = 999L,
            excluidaEmMillis = 2000L
        )

        assertEquals(0, exclusaoDao.buscarTodas().size)
    }

    @Test
    fun excluirMovimentacaoInexistenteNaoDeveRegistrarExclusao() = runBlocking {
        val movimentacaoDao = database.movimentacaoDao()
        val exclusaoDao = database.exclusaoPendenteDao()

        movimentacaoDao.excluirComRegistroPendente(
            id = 999L,
            excluidaEmMillis = 2000L
        )

        assertEquals(0, exclusaoDao.buscarTodas().size)
    }
}