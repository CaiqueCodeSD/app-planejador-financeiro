package com.example.planejadorfinanceiro.di

import com.example.planejadorfinanceiro.presentation.conta.ContaViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        ContaViewModel(get())
    }
}
