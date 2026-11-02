package com.example.planejadorfinanceiro.data.repository

import com.example.planejadorfinanceiro.data.local.ExclusaoPendenteDao
import com.example.planejadorfinanceiro.domain.model.ExclusaoPendente
import com.example.planejadorfinanceiro.domain.repository.ExclusaoPendenteRepository

class ExclusaoPendenteRepositoryImpl(
    private val exclusaoPendenteDao: ExclusaoPendenteDao
) : ExclusaoPendenteRepository {

    override suspend fun buscarTodas(): List<ExclusaoPendente> {
        return exclusaoPendenteDao.buscarTodas().map { entity ->
            ExclusaoPendente(
                uuid = entity.uuid,
                excluidaEmMillis = entity.excluidaEmMillis,
                tipoRegistro = entity.tipoRegistro
            )
        }
    }

    override suspend fun confirmarSincronizacao(
        uuid: String,
        excluidaEmMillis: Long
    ): Boolean {
        val linhasRemovidas = exclusaoPendenteDao.remover(
            uuid = uuid,
            excluidaEmMillis = excluidaEmMillis
        )

        return linhasRemovidas > 0
    }
}