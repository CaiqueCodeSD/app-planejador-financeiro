package com.example.planejadorfinanceiro.di

import com.example.planejadorfinanceiro.data.remote.GoogleSheetsHttpClient
import com.example.planejadorfinanceiro.data.remote.GoogleSheetsSincronizacaoDataSource
import com.example.planejadorfinanceiro.data.repository.ContaRepositoryImpl
import com.example.planejadorfinanceiro.data.repository.ExclusaoPendenteRepositoryImpl
import com.example.planejadorfinanceiro.data.repository.GoogleAuthorizationRepositoryImpl
import com.example.planejadorfinanceiro.data.repository.GoogleSheetsConfigRepositoryImpl
import com.example.planejadorfinanceiro.data.repository.GoogleSheetsRepositoryImpl
import com.example.planejadorfinanceiro.data.repository.ModoAcessoRepositoryImpl
import com.example.planejadorfinanceiro.data.repository.MovimentacaoRepositoryImpl
import com.example.planejadorfinanceiro.domain.repository.ContaRepository
import com.example.planejadorfinanceiro.domain.repository.ExclusaoPendenteRepository
import com.example.planejadorfinanceiro.domain.repository.GoogleAuthorizationRepository
import com.example.planejadorfinanceiro.domain.repository.GoogleSheetsConfigRepository
import com.example.planejadorfinanceiro.domain.repository.GoogleSheetsRepository
import com.example.planejadorfinanceiro.domain.repository.ModoAcessoRepository
import com.example.planejadorfinanceiro.domain.repository.MovimentacaoRepository
import com.example.planejadorfinanceiro.domain.repository.SincronizacaoDataSource
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.example.planejadorfinanceiro.data.repository.SincronizacaoRepositoryImpl
import com.example.planejadorfinanceiro.domain.repository.SincronizacaoRepository

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

    single<GoogleAuthorizationRepository> {
        GoogleAuthorizationRepositoryImpl(androidContext())
    }

    single<GoogleSheetsRepository> {
        GoogleSheetsRepositoryImpl(get())
    }

    single<GoogleSheetsConfigRepository> {
        GoogleSheetsConfigRepositoryImpl(get())
    }

    single {
        OkHttpClient.Builder().build()
    }

    single {
        GoogleSheetsHttpClient(get())
    }

    single<SincronizacaoDataSource> {
        GoogleSheetsSincronizacaoDataSource(
            authorizationRepository = get(),
            configRepository = get(),
            httpClient = get()
        )
    }

    single<SincronizacaoRepository> {
        SincronizacaoRepositoryImpl(
            contaRepository = get(),
            movimentacaoRepository = get(),
            exclusaoPendenteRepository = get(),
            sincronizacaoDataSource = get()
        )
    }

}
