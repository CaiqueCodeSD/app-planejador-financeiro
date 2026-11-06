package com.example.planejadorfinanceiro.data.repository

import com.example.planejadorfinanceiro.domain.model.EstadoSincronizacao
import com.example.planejadorfinanceiro.domain.repository.ContaRepository
import com.example.planejadorfinanceiro.domain.repository.ExclusaoPendenteRepository
import com.example.planejadorfinanceiro.domain.repository.MovimentacaoRepository
import com.example.planejadorfinanceiro.domain.repository.SincronizacaoRepository
import com.example.planejadorfinanceiro.domain.repository.SincronizacaoDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

class SincronizacaoRepositoryImpl(
    private val contaRepository: ContaRepository,
    private val movimentacaoRepository: MovimentacaoRepository,
    private val exclusaoPendenteRepository: ExclusaoPendenteRepository,
    private val sincronizacaoDataSource: SincronizacaoDataSource
) : SincronizacaoRepository {

    private val _estado =
        MutableStateFlow<EstadoSincronizacao>(EstadoSincronizacao.Ociosa)

    private val mutex = Mutex()

    override val estado: StateFlow<EstadoSincronizacao> =
        _estado.asStateFlow()

    override suspend fun sincronizar() {
        if (!mutex.tryLock()) return

        try {
            _estado.value = EstadoSincronizacao.EmAndamento

            var totalSincronizado = 0

            val exclusoes = exclusaoPendenteRepository.buscarTodas()

            for (exclusao in exclusoes) {
                sincronizacaoDataSource.sincronizarExclusao(exclusao)

                val confirmada =
                    exclusaoPendenteRepository.confirmarSincronizacao(
                        uuid = exclusao.uuid,
                        excluidaEmMillis = exclusao.excluidaEmMillis
                    )

                if (confirmada) {
                    totalSincronizado++
                }
            }

            val contas = contaRepository.buscarPendentesSincronizacao()

            for (conta in contas) {
                sincronizacaoDataSource.sincronizarConta(conta)

                val confirmada = contaRepository.marcarComoSincronizada(
                    uuid = conta.uuid,
                    versao = conta.versao
                )

                if (confirmada) {
                    totalSincronizado++
                }
            }

            val movimentacoes =
                movimentacaoRepository.buscarPendentesSincronizacao()

            for (movimentacao in movimentacoes) {
                sincronizacaoDataSource.sincronizarMovimentacao(movimentacao)

                val confirmada =
                    movimentacaoRepository.marcarComoSincronizada(
                        uuid = movimentacao.uuid,
                        versao = movimentacao.versao
                    )

                if (confirmada) {
                    totalSincronizado++
                }
            }

            _estado.value =
                EstadoSincronizacao.Concluida(totalSincronizado)

        } catch (e: CancellationException) {
            _estado.value = EstadoSincronizacao.Ociosa
            throw e

        } catch (e: Exception) {
            _estado.value = EstadoSincronizacao.Erro(
                e.message ?: "Não foi possível concluir a sincronização."
            )

        } finally {
            mutex.unlock()
        }
    }
}
