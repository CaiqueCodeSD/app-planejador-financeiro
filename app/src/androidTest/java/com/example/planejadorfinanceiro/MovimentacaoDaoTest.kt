package com.example.planejadorfinanceiro.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovimentacaoDaoTest {

    private lateinit var database: FinanceiroDatabase
    private lateinit var movimentacaoDao: MovimentacaoDao

    @Before
    fun configurar() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            FinanceiroDatabase::class.java
        ).allowMainThreadQueries().build()

        movimentacaoDao = database.movimentacaoDao()
    }

    @After
    fun encerrar() {
        database.close()
    }

    @Test
    fun atualizar_deveIncrementarVersaoERetornarParaPendente() =
        runBlocking {
            val movimentacao = criarMovimentacao()
            val id = movimentacaoDao.inserir(movimentacao)

            movimentacaoDao.marcarComoSincronizada(
                uuid = movimentacao.uuid,
                versao = 1L
            )

            val resultado = movimentacaoDao.atualizar(
                id = id,
                descricao = "Compra atualizada",
                valorCentavos = 25000L,
                tipo = "DESPESA",
                dataMillis = 1794000000000L,
                categoria = "Alimentação",
                atualizadaEmMillis = 2000L
            )

            val atualizada = movimentacaoDao.buscarPorId(id)

            assertEquals(1, resultado)
            assertEquals("Compra atualizada", atualizada?.descricao)
            assertEquals(25000L, atualizada?.valorCentavos)
            assertEquals(2000L, atualizada?.atualizadaEmMillis)
            assertEquals("PENDENTE", atualizada?.statusSincronizacao)
            assertEquals(2L, atualizada?.versao)
        }

    @Test
    fun confirmarVersaoAntiga_naoDeveMarcarMovimentacaoAtualizadaComoSincronizada() =
        runBlocking {
            val movimentacao = criarMovimentacao()
            val id = movimentacaoDao.inserir(movimentacao)

            // Simula uma edição enquanto a versão 1 está sendo enviada.
            movimentacaoDao.atualizar(
                id = id,
                descricao = "Compra atualizada",
                valorCentavos = 25000L,
                tipo = "DESPESA",
                dataMillis = 1794000000000L,
                categoria = "Alimentação",
                atualizadaEmMillis = 2000L
            )

            val resultadoAntigo =
                movimentacaoDao.marcarComoSincronizada(
                    uuid = movimentacao.uuid,
                    versao = 1L
                )

            val aposConfirmacaoAntiga =
                movimentacaoDao.buscarPorId(id)

            assertEquals(0, resultadoAntigo)
            assertEquals(2L, aposConfirmacaoAntiga?.versao)
            assertEquals(
                "PENDENTE",
                aposConfirmacaoAntiga?.statusSincronizacao
            )

            // A versão atual pode ser confirmada normalmente.
            val resultadoAtual =
                movimentacaoDao.marcarComoSincronizada(
                    uuid = movimentacao.uuid,
                    versao = 2L
                )

            val aposConfirmacaoAtual =
                movimentacaoDao.buscarPorId(id)

            assertEquals(1, resultadoAtual)
            assertEquals(
                "SINCRONIZADA",
                aposConfirmacaoAtual?.statusSincronizacao
            )
        }

    @Test
    fun atualizar_naoDeveAlterarMovimentacaoInexistente() =
        runBlocking {
            val resultado = movimentacaoDao.atualizar(
                id = 999L,
                descricao = "Inexistente",
                valorCentavos = 10000L,
                tipo = "DESPESA",
                dataMillis = 1794000000000L,
                categoria = "Outros",
                atualizadaEmMillis = 2000L
            )

            assertEquals(0, resultado)
        }

    private fun criarMovimentacao() = MovimentacaoEntity(
        uuid = "movimentacao-teste-uuid",
        descricao = "Compra original",
        valorCentavos = 15000L,
        tipo = "DESPESA",
        dataMillis = 1793000000000L,
        categoria = "Mercado",
        criadaEmMillis = 1000L,
        atualizadaEmMillis = 1000L,
        statusSincronizacao = "PENDENTE",
        versao = 1L
    )
}
