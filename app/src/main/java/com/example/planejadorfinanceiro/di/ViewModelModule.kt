package com.example.planejadorfinanceiro.di

import com.example.planejadorfinanceiro.presentation.conta.ContaViewModel
import com.example.planejadorfinanceiro.presentation.dashboard.DashboardViewModel
import com.example.planejadorfinanceiro.presentation.movimentacao.MovimentacaoViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        ContaViewModel(get())
    }

    viewModel {
        MovimentacaoViewModel(get())
    }

    viewModel {
        DashboardViewModel(get())
    }
}
