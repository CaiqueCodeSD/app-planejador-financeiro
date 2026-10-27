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

    override fun observarMovimentacoes(): Flow<List<Movimentacao>> =
        movimentacaoDao.observarTodas().map { lista ->
            lista.map { it.toDomain() }
        }

    override fun observarTotalPorTipoNoPeriodo(
        tipo: TipoMovimentacao,
        inicioMillis: Long,
        fimMillis: Long
    ): Flow<Long> =
        movimentacaoDao.observarTotalPorTipoNoPeriodo(
            tipo.name,
            inicioMillis,
            fimMillis
        )

    override suspend fun buscarPorId(id: Long): Movimentacao? =
        movimentacaoDao.buscarPorId(id)?.toDomain()

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

    override suspend fun excluir(id: Long) {
        movimentacaoDao.excluirComRegistroPendente(
            id = id,
            excluidaEmMillis = System.currentTimeMillis()
        )
    }

    override suspend fun buscarPendentesSincronizacao(): List<Movimentacao> =
        movimentacaoDao.buscarPendentesSincronizacao().map { it.toDomain() }

    override suspend fun marcarComoSincronizada(
        uuid: String,
        atualizadaEmMillis: Long
    ) {
        movimentacaoDao.marcarComoSincronizada(uuid, atualizadaEmMillis)
    }

    private fun MovimentacaoEntity.toDomain() = Movimentacao(
        id = id,
        uuid = uuid,
        descricao = descricao,
        valorCentavos = valorCentavos,
        tipo = TipoMovimentacao.valueOf(tipo),
        dataMillis = dataMillis,
        categoria = categoria,
        criadaEmMillis = criadaEmMillis,
        atualizadaEmMillis = atualizadaEmMillis,
        statusSincronizacao = StatusSincronizacao.valueOf(statusSincronizacao)
    )

    private fun Movimentacao.toEntity() = MovimentacaoEntity(
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
