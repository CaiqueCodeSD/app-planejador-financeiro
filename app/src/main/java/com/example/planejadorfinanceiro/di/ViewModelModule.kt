package com.example.planejadorfinanceiro.di

import com.example.planejadorfinanceiro.presentation.acesso.ModoAcessoViewModel
import com.example.planejadorfinanceiro.presentation.bloqueio.BloqueioPinViewModel
import com.example.planejadorfinanceiro.presentation.conta.ContaViewModel
import com.example.planejadorfinanceiro.presentation.dashboard.DashboardViewModel
import com.example.planejadorfinanceiro.presentation.movimentacao.MovimentacaoViewModel
import com.example.planejadorfinanceiro.presentation.seguranca.CadastroPinViewModel
import com.example.planejadorfinanceiro.presentation.sincronizacao.SincronizacaoViewModel
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
            pinRepository = get()
        )
    }

    viewModel {
        CadastroPinViewModel(
            pinRepository = get()
        )
    }

}
