package com.example.planejadorfinanceiro.data.remote

object GoogleSheetsSchema {

    const val ABA_CONTAS = "Contas"
    const val ABA_MOVIMENTACOES = "Movimentações"

    val CABECALHO_CONTAS = listOf(
        "uuid",
        "descricao",
        "valor_centavos",
        "vencimento_millis",
        "situacao",
        "criada_em_millis",
        "atualizada_em_millis",
        "versao"
    )

    val CABECALHO_MOVIMENTACOES = listOf(
        "uuid",
        "descricao",
        "valor_centavos",
        "tipo",
        "data_millis",
        "categoria",
        "criada_em_millis",
        "atualizada_em_millis",
        "versao"
    )
}
