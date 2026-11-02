package com.example.planejadorfinanceiro

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.planejadorfinanceiro.data.local.ContaEntity
import com.example.planejadorfinanceiro.data.local.ExclusaoPendenteEntity
import com.example.planejadorfinanceiro.data.local.FinanceiroDatabase
import com.example.planejadorfinanceiro.data.local.MovimentacaoEntity
import com.example.planejadorfinanceiro.data.repository.ContaRepositoryImpl
import com.example.planejadorfinanceiro.data.repository.ExclusaoPendenteRepositoryImpl
import com.example.planejadorfinanceiro.data.repository.MovimentacaoRepositoryImpl
import com.example.planejadorfinanceiro.data.repository.SincronizacaoRepositoryImpl
import com.example.planejadorfinanceiro.domain.model.EstadoSincronizacao
import com.example.planejadorfinanceiro.domain.model.ExclusaoPendente
import com.example.planejadorfinanceiro.domain.model.StatusSincronizacao
import com.example.planejadorfinanceiro.domain.repository.SincronizacaoDataSource
import com.example.planejadorfinanceiro.domain.repository.SincronizacaoRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SincronizacaoRepositoryTest {

    private lateinit var database: FinanceiroDatabase

    private lateinit var contaRepository: ContaRepositoryImpl
    private lateinit var movimentacaoRepository: MovimentacaoRepositoryImpl
    private lateinit var exclusaoPendenteRepository: ExclusaoPendenteRepositoryImpl

    private lateinit var dataSource: TesteSincronizacaoDataSource
    private lateinit var sincronizacaoRepository: SincronizacaoRepository

    @Before
    fun configurar() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            FinanceiroDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        contaRepository = ContaRepositoryImpl(database.contaDao())

        movimentacaoRepository = MovimentacaoRepositoryImpl(
            database.movimentacaoDao()
        )

        exclusaoPendenteRepository = ExclusaoPendenteRepositoryImpl(
            database.exclusaoPendenteDao()
        )

        dataSource = TesteSincronizacaoDataSource()

        sincronizacaoRepository = SincronizacaoRepositoryImpl(
            contaRepository = contaRepository,
            movimentacaoRepository = movimentacaoRepository,
            exclusaoPendenteRepository = exclusaoPendenteRepository,
            sincronizacaoDataSource = dataSource
        )

    }

    @After
    fun encerrar() {
        database.close()
    }

    private fun inserirDadosDeTeste() = runBlocking {
        database.contaDao().inserir(
            ContaEntity(
                uuid = "uuid-conta-teste",
                descricao = "Conta de energia",
                valorCentavos = 15000L,
                vencimentoMillis = 1794000000000L
            )
        )

        database.movimentacaoDao().inserir(
            MovimentacaoEntity(
                uuid = "uuid-movimentacao-teste",
                descricao = "Salário",
                valorCentavos = 300000L,
                tipo = "RECEITA",
                dataMillis = 1794000000000L,
                categoria = "Salário"
            )
        )

        database.exclusaoPendenteDao().registrar(
            ExclusaoPendenteEntity(
                uuid = "uuid-exclusao-teste",
                excluidaEmMillis = 1794000000000L,
                tipoRegistro = "CONTA"
            )
        )
    }

    @Test
    fun sincronizar_deveProcessarExclusoesContasEMovimentacoes() = runBlocking {
        inserirDadosDeTeste()

        sincronizacaoRepository.sincronizar()

        // Verifica a ordem das operações.
        assertEquals(
            listOf(
                "EXCLUSAO:uuid-exclusao-teste",
                "CONTA:uuid-conta-teste",
                "MOVIMENTACAO:uuid-movimentacao-teste"
            ),
            dataSource.chamadas
        )

        // Verifica se a exclusão foi removida da fila.
        assertTrue(
            exclusaoPendenteRepository.buscarTodas().isEmpty()
        )

        val contaSincronizada = database.contaDao()
            .buscarPorUuid("uuid-conta-teste")

        assertEquals(
            StatusSincronizacao.SINCRONIZADA.name,
            contaSincronizada?.statusSincronizacao
        )

        // Verifica se a movimentação foi sincronizada.
        val movimentacaoSincronizada = database.movimentacaoDao()
            .buscarPorUuid("uuid-movimentacao-teste")

        assertEquals(
            StatusSincronizacao.SINCRONIZADA.name,
            movimentacaoSincronizada?.statusSincronizacao
        )

        assertEquals(
            EstadoSincronizacao.Concluida(totalSincronizado = 3),
            sincronizacaoRepository.estado.value
        )

    }

    @Test
    fun sincronizar_quandoFalhaNaExclusao_deveManterPendencias() = runBlocking {
        inserirDadosDeTeste()

        // Simula uma falha durante o envio da exclusão.
        dataSource.falharEm = OperacaoSincronizacao.EXCLUSAO

        sincronizacaoRepository.sincronizar()

        // Verifica se somente a exclusão foi tentada.
        assertEquals(
            listOf("EXCLUSAO:uuid-exclusao-teste"),
            dataSource.chamadas
        )

        // A exclusão deve continuar na fila.
        assertEquals(
            1,
            exclusaoPendenteRepository.buscarTodas().size
        )

        // A conta deve continuar pendente.
        val conta = database.contaDao()
            .buscarPorUuid("uuid-conta-teste")

        assertEquals(
            StatusSincronizacao.PENDENTE.name,
            conta?.statusSincronizacao
        )

        // A movimentação também deve continuar pendente.
        val movimentacao = database.movimentacaoDao()
            .buscarPorUuid("uuid-movimentacao-teste")

        assertEquals(
            StatusSincronizacao.PENDENTE.name,
            movimentacao?.statusSincronizacao
        )

        // O estado deve indicar erro.
        assertTrue(
            sincronizacaoRepository.estado.value is EstadoSincronizacao.Erro
        )
    }

    @Test
    fun sincronizar_quandoFalhaNaConta_deveManterPendencias() = runBlocking {
        inserirDadosDeTeste()

        // Simula uma falha durante o envio da conta.
        dataSource.falharEm = OperacaoSincronizacao.CONTA

        sincronizacaoRepository.sincronizar()

        // A exclusão deve ter sido processada antes da falha.
        assertEquals(
            listOf(
                "EXCLUSAO:uuid-exclusao-teste",
                "CONTA:uuid-conta-teste"
            ),
            dataSource.chamadas
        )

        // A exclusão já foi confirmada e removida da fila.
        assertTrue(
            exclusaoPendenteRepository.buscarTodas().isEmpty()
        )

        // A conta deve continuar pendente.
        val conta = database.contaDao()
            .buscarPorUuid("uuid-conta-teste")

        assertEquals(
            StatusSincronizacao.PENDENTE.name,
            conta?.statusSincronizacao
        )

        // A movimentação não deve ter sido enviada.
        val movimentacao = database.movimentacaoDao()
            .buscarPorUuid("uuid-movimentacao-teste")

        assertEquals(
            StatusSincronizacao.PENDENTE.name,
            movimentacao?.statusSincronizacao
        )

        // A sincronização deve terminar em estado de erro.
        assertTrue(
            sincronizacaoRepository.estado.value is EstadoSincronizacao.Erro
        )
    }

    @Test
    fun sincronizar_quandoFalhaNaMovimentacao_devePreservarEstadoParcial() = runBlocking {
        inserirDadosDeTeste()

        dataSource.falharEm = OperacaoSincronizacao.MOVIMENTACAO

        sincronizacaoRepository.sincronizar()

        assertEquals(
            listOf(
                "EXCLUSAO:uuid-exclusao-teste",
                "CONTA:uuid-conta-teste",
                "MOVIMENTACAO:uuid-movimentacao-teste"
            ),
            dataSource.chamadas
        )

        assertTrue(
            exclusaoPendenteRepository.buscarTodas().isEmpty()
        )

        val conta = database.contaDao()
            .buscarPorUuid("uuid-conta-teste")

        assertEquals(
            StatusSincronizacao.SINCRONIZADA.name,
            conta?.statusSincronizacao
        )

        val movimentacao = database.movimentacaoDao()
            .buscarPorUuid("uuid-movimentacao-teste")

        assertEquals(
            StatusSincronizacao.PENDENTE.name,
            movimentacao?.statusSincronizacao
        )

        assertTrue(
            sincronizacaoRepository.estado.value is EstadoSincronizacao.Erro
        )
    }

    @Test
    fun sincronizar_quandoCancelada_devePropagarCancelamentoELiberarMutex() = runBlocking {
        inserirDadosDeTeste()

        val sincronizacaoIniciada = CompletableDeferred<Unit>()

        val dataSourceCancelavel = object : SincronizacaoDataSource {

            override suspend fun sincronizarExclusao(
                exclusao: ExclusaoPendente
            ) {
                sincronizacaoIniciada.complete(Unit)
                kotlinx.coroutines.awaitCancellation()
            }

            override suspend fun sincronizarConta(
                conta: com.example.planejadorfinanceiro.domain.model.Conta
            ) {
                error("Não deveria processar contas.")
            }

            override suspend fun sincronizarMovimentacao(
                movimentacao: com.example.planejadorfinanceiro.domain.model.Movimentacao
            ) {
                error("Não deveria processar movimentações.")
            }
        }

        val repository = SincronizacaoRepositoryImpl(
            contaRepository,
            movimentacaoRepository,
            exclusaoPendenteRepository,
            dataSourceCancelavel
        )

        val tarefa = launch {
            repository.sincronizar()
        }

        sincronizacaoIniciada.await()

        assertEquals(
            EstadoSincronizacao.EmAndamento,
            repository.estado.value
        )

        tarefa.cancelAndJoin()

        assertEquals(
            EstadoSincronizacao.Ociosa,
            repository.estado.value
        )

        assertEquals(
            1,
            exclusaoPendenteRepository.buscarTodas().size
        )
    }
}
