package com.example.planejadorfinanceiro

import com.example.planejadorfinanceiro.data.remote.GoogleSheetsMapper
import com.example.planejadorfinanceiro.domain.model.Conta
import com.example.planejadorfinanceiro.domain.model.Movimentacao
import com.example.planejadorfinanceiro.domain.model.SituacaoConta
import com.example.planejadorfinanceiro.domain.model.TipoMovimentacao
import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleSheetsMapperTest {

    @Test
    fun deveMapearTodosOsCamposDaConta() {
        val conta = Conta(
            descricao = "Conta de energia",
            valorCentavos = 15990L,
            vencimentoMillis = 1764547200000L,
            situacao = SituacaoConta.PENDENTE,
            criadaEmMillis = 1764000000000L,
            uuid = "uuid-conta-001",
            atualizadaEmMillis = 1764100000000L,
            versao = 2L
        )

        val resultado = GoogleSheetsMapper.mapearConta(conta)

        assertEquals(
            listOf(
                "uuid-conta-001",
                "Conta de energia",
                15990L,
                1764547200000L,
                "PENDENTE",
                1764000000000L,
                1764100000000L,
                2L
            ),
            resultado
        )
    }

    @Test
    fun deveMapearTodosOsCamposDaMovimentacao() {
        val movimentacao = Movimentacao(
            uuid = "uuid-mov-001",
            descricao = "Salário",
            valorCentavos = 350000L,
            tipo = TipoMovimentacao.RECEITA,
            dataMillis = 1764547200000L,
            categoria = "Trabalho",
            criadaEmMillis = 1764000000000L,
            atualizadaEmMillis = 1764100000000L,
            versao = 3L
        )

        val resultado =
            GoogleSheetsMapper.mapearMovimentacao(movimentacao)

        assertEquals(
            listOf(
                "uuid-mov-001",
                "Salário",
                350000L,
                "RECEITA",
                1764547200000L,
                "Trabalho",
                1764000000000L,
                1764100000000L,
                3L
            ),
            resultado
        )
    }
}
