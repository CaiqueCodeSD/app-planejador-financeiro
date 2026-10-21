package com.example.planejadorfinanceiro.data.repository

import com.example.planejadorfinanceiro.data.local.ContaDao
import com.example.planejadorfinanceiro.data.local.ContaEntity
import com.example.planejadorfinanceiro.domain.model.Conta
import com.example.planejadorfinanceiro.domain.model.SituacaoConta
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
        val entity = conta.toEntity()

        return if (conta.id == 0L) {
            contaDao.inserir(entity)
        } else {
            contaDao.atualizar(entity)
            conta.id
        }
    }

    override suspend fun marcarComoPaga(id: Long) {
        contaDao.marcarComoPaga(id)
    }

    override suspend fun excluir(id: Long) {
        contaDao.excluir(id)
    }

    private fun ContaEntity.toDomain(): Conta {
        return Conta(
            id = id,
            descricao = descricao,
            valorCentavos = valorCentavos,
            vencimentoMillis = vencimentoMillis,
            situacao = SituacaoConta.valueOf(situacao),
            criadaEmMillis = criadaEmMillis
        )
    }

    private fun Conta.toEntity(): ContaEntity {
        return ContaEntity(
            id = id,
            descricao = descricao,
            valorCentavos = valorCentavos,
            vencimentoMillis = vencimentoMillis,
            situacao = situacao.name,
            criadaEmMillis = criadaEmMillis
        )
    }
}
