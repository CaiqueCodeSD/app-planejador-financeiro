package com.example.planejadorfinanceiro.presentation.conta

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.planejadorfinanceiro.data.local.FinanceiroDatabase
import com.example.planejadorfinanceiro.data.repository.ContaRepositoryImpl

class ContaViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {

    private val repository = ContaRepositoryImpl(
        FinanceiroDatabase.getInstance(context).contaDao()
    )

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(ContaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContaViewModel(repository) as T
        }

        throw IllegalArgumentException(
            "ViewModel desconhecido: ${modelClass.name}"
        )
    }
}
