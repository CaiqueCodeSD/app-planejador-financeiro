package com.example.planejadorfinanceiro.data.repository

import com.example.planejadorfinanceiro.data.local.ContaDao
import com.example.planejadorfinanceiro.data.local.ContaEntity
import com.example.planejadorfinanceiro.domain.model.Conta
import com.example.planejadorfinanceiro.domain.model.SituacaoConta
import com.example.planejadorfinanceiro.domain.model.StatusSincronizacao
import com.example.planejadorfinanceiro.domain.repository.ContaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContaRepositoryImpl(
    private val contaDao: ContaDao
) : ContaRepository {

    override fun observarContas(): Flow<List<Conta>> {
        return contaDao.observarTodas().map { contas ->
            contas.map { it.toDomain() }
        }
    }

    override suspend fun buscarPorId(id: Long): Conta? {
        return contaDao.buscarPorId(id)?.toDomain()
    }

    override suspend fun salvar(conta: Conta): Long {
        val contaAtualizada = conta.copy(
            atualizadaEmMillis = System.currentTimeMillis(),
            statusSincronizacao = StatusSincronizacao.PENDENTE
        )

        val entity = contaAtualizada.toEntity()

        return if (contaAtualizada.id == 0L) {
            contaDao.inserir(entity)
        } else {
            contaDao.atualizar(
                id = entity.id,
                descricao = entity.descricao,
                valorCentavos = entity.valorCentavos,
                vencimentoMillis = entity.vencimentoMillis,
                situacao = entity.situacao,
                atualizadaEmMillis = entity.atualizadaEmMillis
            )

            contaAtualizada.id
        }
    }

    override suspend fun marcarComoPaga(id: Long) {
        contaDao.marcarComoPaga(
            id = id,
            atualizadaEmMillis = System.currentTimeMillis()
        )
    }

    override suspend fun excluir(id: Long) {
        contaDao.excluirComRegistroPendente(
            id = id,
            excluidaEmMillis = System.currentTimeMillis()
        )
    }

    override suspend fun buscarPendentesSincronizacao(): List<Conta> {
        return contaDao.buscarPendentesSincronizacao().map {
            it.toDomain()
        }
    }

    override suspend fun marcarComoSincronizada(
        uuid: String,
        versao: Long
    ): Boolean {
        return contaDao.marcarComoSincronizada(
            uuid = uuid,
            versao = versao
        ) > 0
    }
}

private fun ContaEntity.toDomain(): Conta {
    return Conta(
        id = id,
        uuid = uuid,
        descricao = descricao,
        valorCentavos = valorCentavos,
        vencimentoMillis = vencimentoMillis,
        situacao = SituacaoConta.valueOf(situacao),
        criadaEmMillis = criadaEmMillis,
        atualizadaEmMillis = atualizadaEmMillis,
        statusSincronizacao = StatusSincronizacao.valueOf(statusSincronizacao),
        versao = versao
    )
}

private fun Conta.toEntity(): ContaEntity {
    return ContaEntity(
        id = id,
        uuid = uuid,
        descricao = descricao,
        valorCentavos = valorCentavos,
        vencimentoMillis = vencimentoMillis,
        situacao = situacao.name,
        criadaEmMillis = criadaEmMillis,
        atualizadaEmMillis = atualizadaEmMillis,
        statusSincronizacao = statusSincronizacao.name,
        versao = versao
    )
}
