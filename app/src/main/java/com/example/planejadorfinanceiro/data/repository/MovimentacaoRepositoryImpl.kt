
package com.example.planejadorfinanceiro.data.repository

import com.example.planejadorfinanceiro.data.local.MovimentacaoDao
import com.example.planejadorfinanceiro.data.local.MovimentacaoEntity
import com.example.planejadorfinanceiro.domain.model.Movimentacao
import com.example.planejadorfinanceiro.domain.model.StatusSincronizacao
import com.example.planejadorfinanceiro.domain.model.TipoMovimentacao
import com.example.planejadorfinanceiro.domain.repository.MovimentacaoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MovimentacaoRepositoryImpl(
    private val movimentacaoDao: MovimentacaoDao
) : MovimentacaoRepository {

    override fun observarMovimentacoes(): Flow<List<Movimentacao>> {
        return movimentacaoDao.observarTodas().map { movimentacoes ->
            movimentacoes.map { it.toDomain() }
        }
    }

    override fun observarTotalPorTipoNoPeriodo(
        tipo: TipoMovimentacao,
        inicioMillis: Long,
        fimMillis: Long
    ): Flow<Long> {
        return movimentacaoDao.observarTotalPorTipoNoPeriodo(
            tipo = tipo.name,
            inicioMillis = inicioMillis,
            fimMillis = fimMillis
        )
    }

    override suspend fun buscarPorId(id: Long): Movimentacao? {
        return movimentacaoDao.buscarPorId(id)?.toDomain()
    }

    override suspend fun salvar(movimentacao: Movimentacao): Long {
        val movimentacaoAtualizada = movimentacao.copy(
            atualizadaEmMillis = System.currentTimeMillis(),
            statusSincronizacao = StatusSincronizacao.PENDENTE
        )

        val entity = movimentacaoAtualizada.toEntity()

        return if (movimentacaoAtualizada.id == 0L) {
            movimentacaoDao.inserir(entity)
        } else {
            movimentacaoDao.atualizar(entity)
            movimentacaoAtualizada.id
        }
    }

    override suspend fun buscarPendentesSincronizacao(): List<Movimentacao> {
        return movimentacaoDao.buscarPendentesSincronizacao()
            .map { it.toDomain() }
    }

    override suspend fun marcarComoSincronizada(
        uuid: String,
        atualizadaEmMillis: Long
    ) {
        movimentacaoDao.marcarComoSincronizada(
            uuid = uuid,
            atualizadaEmMillis = atualizadaEmMillis
        )
    }

    private fun MovimentacaoEntity.toDomain(): Movimentacao {
        return Movimentacao(
            id = id,
            uuid = uuid,
            descricao = descricao,
            valorCentavos = valorCentavos,
            tipo = TipoMovimentacao.valueOf(tipo),
            dataMillis = dataMillis,
            categoria = categoria,
            criadaEmMillis = criadaEmMillis,
            atualizadaEmMillis = atualizadaEmMillis,
            statusSincronizacao = StatusSincronizacao.valueOf(
                statusSincronizacao
            )
        )
    }

    private fun Movimentacao.toEntity(): MovimentacaoEntity {
        return MovimentacaoEntity(
            id = id,
            uuid = uuid,
            descricao = descricao,
            valorCentavos = valorCentavos,
            tipo = tipo.name,
            dataMillis = dataMillis,
            categoria = categoria,
            criadaEmMillis = criadaEmMillis,
            atualizadaEmMillis = atualizadaEmMillis,
            statusSincronizacao = statusSincronizacao.name
        )
    }
}
