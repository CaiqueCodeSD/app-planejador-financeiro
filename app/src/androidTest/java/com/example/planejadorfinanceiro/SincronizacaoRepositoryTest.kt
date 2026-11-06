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
import kotlinx.coroutines.awaitCancellation
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

        val contaSincronizada = database.contaDao()
            .buscarPorUuid("uuid-conta-teste")

        assertEquals(
            StatusSincronizacao.SINCRONIZADA.name,
            contaSincronizada?.statusSincronizacao
        )

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

        dataSource.falharEm = OperacaoSincronizacao.EXCLUSAO

        sincronizacaoRepository.sincronizar()

        assertEquals(
            listOf("EXCLUSAO:uuid-exclusao-teste"),
            dataSource.chamadas
        )

        assertEquals(
            1,
            exclusaoPendenteRepository.buscarTodas().size
        )

        val conta = database.contaDao()
            .buscarPorUuid("uuid-conta-teste")

        assertEquals(
            StatusSincronizacao.PENDENTE.name,
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
    fun sincronizar_quandoFalhaNaConta_deveManterPendencias() = runBlocking {
        inserirDadosDeTeste()

        dataSource.falharEm = OperacaoSincronizacao.CONTA

        sincronizacaoRepository.sincronizar()

        assertEquals(
            listOf(
                "EXCLUSAO:uuid-exclusao-teste",
                "CONTA:uuid-conta-teste"
            ),
            dataSource.chamadas
        )

        assertTrue(
            exclusaoPendenteRepository.buscarTodas().isEmpty()
        )

        val conta = database.contaDao()
            .buscarPorUuid("uuid-conta-teste")

        assertEquals(
            StatusSincronizacao.PENDENTE.name,
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
    fun sincronizar_quandoContaExcluidaDuranteEnvio_devePreservarExclusaoPendente() = runBlocking {
        val contaId = database.contaDao().inserir(
            ContaEntity(
                uuid = "uuid-conta-concorrencia",
                descricao = "Conta de teste",
                valorCentavos = 15000L,
                vencimentoMillis = 1794000000000L
            )
        )

        val envioIniciado = CompletableDeferred<Unit>()
        val liberarEnvio = CompletableDeferred<Unit>()
        val chamadas = mutableListOf<String>()

        val dataSourceControlado = object : SincronizacaoDataSource {

            override suspend fun sincronizarConta(
                conta: com.example.planejadorfinanceiro.domain.model.Conta
            ) {
                chamadas.add("CONTA:${conta.uuid}")
                envioIniciado.complete(Unit)
                liberarEnvio.await()
            }

            override suspend fun sincronizarMovimentacao(
                movimentacao: com.example.planejadorfinanceiro.domain.model.Movimentacao
            ) {
                error("Não deveria processar movimentações.")
            }

            override suspend fun sincronizarExclusao(
                exclusao: ExclusaoPendente
            ) {
                chamadas.add("EXCLUSAO:${exclusao.uuid}")
            }
        }

        val repository = SincronizacaoRepositoryImpl(
            contaRepository,
            movimentacaoRepository,
            exclusaoPendenteRepository,
            dataSourceControlado
        )

        val tarefa = launch {
            repository.sincronizar()
        }

        envioIniciado.await()

        database.contaDao().excluirComRegistroPendente(
            id = contaId,
            excluidaEmMillis = 1795000000000L
        )

        assertEquals(
            null,
            database.contaDao().buscarPorUuid("uuid-conta-concorrencia")
        )

        liberarEnvio.complete(Unit)
        tarefa.join()

        val exclusoes = exclusaoPendenteRepository.buscarTodas()

        assertEquals(1, exclusoes.size)

        assertEquals(
            "uuid-conta-concorrencia",
            exclusoes.first().uuid
        )

        assertEquals("CONTA", exclusoes.first().tipoRegistro)

        assertEquals(
            EstadoSincronizacao.Concluida(totalSincronizado = 0),
            repository.estado.value
        )

        repository.sincronizar()

        assertEquals(
            listOf(
                "CONTA:uuid-conta-concorrencia",
                "EXCLUSAO:uuid-conta-concorrencia"
            ),
            chamadas
        )

        assertTrue(
            exclusaoPendenteRepository.buscarTodas().isEmpty()
        )

        assertEquals(
            EstadoSincronizacao.Concluida(totalSincronizado = 1),
            repository.estado.value
        )
    }

    @Test
    fun sincronizar_quandoContaEditadaDuranteEnvio_deveManterNovaVersaoPendente() = runBlocking {
        val contaDao = database.contaDao()

        val contaId = contaDao.inserir(
            ContaEntity(
                uuid = "uuid-conta-edicao",
                descricao = "Conta original",
                valorCentavos = 15000L,
                vencimentoMillis = 1794000000000L,
                atualizadaEmMillis = 1000L
            )
        )

        val envioIniciado = CompletableDeferred<Unit>()
        val liberarEnvio = CompletableDeferred<Unit>()

        val versoesEnviadas = mutableListOf<Pair<String, Long>>()

        val dataSourceControlado = object : SincronizacaoDataSource {

            override suspend fun sincronizarConta(
                conta: com.example.planejadorfinanceiro.domain.model.Conta
            ) {
                versoesEnviadas.add(
                    conta.descricao to conta.versao
                )

                if (versoesEnviadas.size == 1) {
                    envioIniciado.complete(Unit)
                    liberarEnvio.await()
                }
            }

            override suspend fun sincronizarMovimentacao(
                movimentacao: com.example.planejadorfinanceiro.domain.model.Movimentacao
            ) {
                error("Não deveria processar movimentações.")
            }

            override suspend fun sincronizarExclusao(
                exclusao: ExclusaoPendente
            ) {
                error("Não deveria processar exclusões.")
            }
        }

        val repository = SincronizacaoRepositoryImpl(
            contaRepository,
            movimentacaoRepository,
            exclusaoPendenteRepository,
            dataSourceControlado
        )

        val tarefa = launch {
            repository.sincronizar()
        }

        envioIniciado.await()

        // Atualização direta pelo DAO, que incrementa a versão atomicamente.
        contaDao.atualizar(
            id = contaId,
            descricao = "Conta atualizada",
            valorCentavos = 20000L,
            vencimentoMillis = 1794000000000L,
            situacao = "PENDENTE",
            atualizadaEmMillis = 2000L
        )

        liberarEnvio.complete(Unit)
        tarefa.join()

        val contaAposPrimeiroEnvio = contaDao.buscarPorUuid(
            "uuid-conta-edicao"
        )

        assertEquals(
            "Conta atualizada",
            contaAposPrimeiroEnvio?.descricao
        )

        assertEquals(
            20000L,
            contaAposPrimeiroEnvio?.valorCentavos
        )

        assertEquals(
            2000L,
            contaAposPrimeiroEnvio?.atualizadaEmMillis
        )

        assertEquals(
            2L,
            contaAposPrimeiroEnvio?.versao
        )

        assertEquals(
            StatusSincronizacao.PENDENTE.name,
            contaAposPrimeiroEnvio?.statusSincronizacao
        )

        assertEquals(
            EstadoSincronizacao.Concluida(totalSincronizado = 0),
            repository.estado.value
        )

        repository.sincronizar()

        assertEquals(
            listOf(
                "Conta original" to 1L,
                "Conta atualizada" to 2L
            ),
            versoesEnviadas
        )

        val contaFinal = contaDao.buscarPorUuid("uuid-conta-edicao")

        assertEquals(
            "Conta atualizada",
            contaFinal?.descricao
        )

        assertEquals(
            2L,
            contaFinal?.versao
        )

        assertEquals(
            StatusSincronizacao.SINCRONIZADA.name,
            contaFinal?.statusSincronizacao
        )

        assertEquals(
            EstadoSincronizacao.Concluida(totalSincronizado = 1),
            repository.estado.value
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
                awaitCancellation()
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
