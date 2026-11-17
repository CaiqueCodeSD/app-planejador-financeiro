package com.example.planejadorfinanceiro.di

import com.example.planejadorfinanceiro.presentation.acesso.ModoAcessoViewModel
import com.example.planejadorfinanceiro.presentation.conta.ContaViewModel
import com.example.planejadorfinanceiro.presentation.dashboard.DashboardViewModel
import com.example.planejadorfinanceiro.presentation.movimentacao.MovimentacaoViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.example.planejadorfinanceiro.presentation.sincronizacao.SincronizacaoViewModel
import com.example.planejadorfinanceiro.presentation.seguranca.BloqueioPinViewModel

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

    viewModel {
        ModoAcessoViewModel(get())
    }

    viewModel {
        SincronizacaoViewModel(
            repository = get(),
            modoAcessoRepository = get()
        )
    }

    viewModel {
        BloqueioPinViewModel(
            validarPinUseCase = get(),
            gerenciadorBloqueioPin = get()
        )
    }

}
