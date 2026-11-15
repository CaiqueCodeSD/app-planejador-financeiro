package com.example.planejadorfinanceiro.di

import com.example.planejadorfinanceiro.domain.usecase.ConectarGoogleUseCase
import org.koin.dsl.module

val useCaseModule = module {

    single {
        ConectarGoogleUseCase(
            googleSheetsRepository = get(),
            googleSheetsConfigRepository = get(),
            modoAcessoRepository = get()
        )
    }
}
