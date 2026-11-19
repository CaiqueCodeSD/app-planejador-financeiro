package com.example.planejadorfinanceiro.di

import com.example.planejadorfinanceiro.domain.usecase.ConectarGoogleUseCase
import org.koin.dsl.module
import com.example.planejadorfinanceiro.domain.usecase.pin.CadastrarPinUseCase
import com.example.planejadorfinanceiro.domain.usecase.pin.ValidarPinUseCase
import com.example.planejadorfinanceiro.domain.usecase.pin.GerenciarPinUseCase

val useCaseModule = module {

    single {
        ConectarGoogleUseCase(
            googleSheetsRepository = get(),
            googleSheetsConfigRepository = get(),
            modoAcessoRepository = get()
        )
    }

    single {
        CadastrarPinUseCase(
            pinRepository = get()
        )
    }

    single {
        ValidarPinUseCase(
            pinRepository = get()
        )
    }

    single {
        GerenciarPinUseCase(
            pinRepository = get()
        )
    }

}
