package com.example.planejadorfinanceiro.di

import com.example.planejadorfinanceiro.data.repository.ExclusaoPendenteRepositoryImpl
import com.example.planejadorfinanceiro.domain.repository.ExclusaoPendenteRepository
import com.example.planejadorfinanceiro.data.repository.ContaRepositoryImpl
import com.example.planejadorfinanceiro.data.repository.MovimentacaoRepositoryImpl
import com.example.planejadorfinanceiro.domain.repository.ContaRepository
import com.example.planejadorfinanceiro.domain.repository.MovimentacaoRepository
import org.koin.dsl.module
import com.example.planejadorfinanceiro.data.repository.ModoAcessoRepositoryImpl
import com.example.planejadorfinanceiro.domain.repository.ModoAcessoRepository

val repositoryModule = module {
    single<ContaRepository> {
        ContaRepositoryImpl(get())
    }

    single<MovimentacaoRepository> {
        MovimentacaoRepositoryImpl(get())
    }

    single<ExclusaoPendenteRepository> {
        ExclusaoPendenteRepositoryImpl(get())
    }

    single<ModoAcessoRepository> {
        ModoAcessoRepositoryImpl(get())
    }

}
