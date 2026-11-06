package com.example.planejadorfinanceiro

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.example.planejadorfinanceiro.data.local.ContaDao
import com.example.planejadorfinanceiro.data.local.ContaEntity
import com.example.planejadorfinanceiro.data.local.FinanceiroDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContaDaoTest {

    private lateinit var database: FinanceiroDatabase
    private lateinit var contaDao: ContaDao

    @Before
    fun configurar() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            FinanceiroDatabase::class.java
        ).allowMainThreadQueries().build()

        contaDao = database.contaDao()
    }

    @After
    fun encerrar() {
        database.close()
    }

    @Test
    fun marcarComoPaga_deveIncrementarVersaoERetornarParaPendente() =
        runBlocking {
            val conta = criarConta()
            val id = contaDao.inserir(conta)

            val resultado = contaDao.marcarComoPaga(
                id = id,
                atualizadaEmMillis = 2000L
            )

            val atualizada = contaDao.buscarPorId(id)

            assertEquals(1, resultado)
            assertEquals("PAGA", atualizada?.situacao)
            assertEquals(2000L, atualizada?.atualizadaEmMillis)
            assertEquals("PENDENTE", atualizada?.statusSincronizacao)
            assertEquals(2L, atualizada?.versao)
        }

    @Test
    fun marcarComoPaga_naoDeveAlterarContaInexistente() =
        runBlocking {
            val resultado = contaDao.marcarComoPaga(
                id = 999L,
                atualizadaEmMillis = 2000L
            )

            assertEquals(0, resultado)
        }

    @Test
    fun confirmarVersaoAntiga_naoDeveMarcarContaAtualizadaComoSincronizada() =
        runBlocking {
            val conta = criarConta().copy(
                statusSincronizacao = "PENDENTE",
                versao = 1L
            )

            val id = contaDao.inserir(conta)

            // Simula uma edição enquanto a versão 1 está sendo enviada.
            contaDao.marcarComoPaga(
                id = id,
                atualizadaEmMillis = 2000L
            )

            // A confirmação da versão antiga não pode atualizar a versão 2.
            val resultadoAntigo = contaDao.marcarComoSincronizada(
                uuid = conta.uuid,
                versao = 1L
            )

            val aposConfirmacaoAntiga = contaDao.buscarPorId(id)

            assertEquals(0, resultadoAntigo)
            assertEquals(2L, aposConfirmacaoAntiga?.versao)
            assertEquals("PENDENTE", aposConfirmacaoAntiga?.statusSincronizacao)

            // A versão atual pode ser confirmada normalmente.
            val resultadoAtual = contaDao.marcarComoSincronizada(
                uuid = conta.uuid,
                versao = 2L
            )

            val aposConfirmacaoAtual = contaDao.buscarPorId(id)

            assertEquals(1, resultadoAtual)
            assertEquals("SINCRONIZADA", aposConfirmacaoAtual?.statusSincronizacao)
        }

    private fun criarConta() = ContaEntity(
        descricao = "Conta de teste",
        valorCentavos = 15000L,
        vencimentoMillis = 1794000000000L,
        situacao = "PENDENTE",
        criadaEmMillis = 1000L,
        uuid = "conta-teste-uuid",
        atualizadaEmMillis = 1000L,
        statusSincronizacao = "PENDENTE",
        versao = 1L
    )
}
