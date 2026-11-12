package com.example.planejadorfinanceiro.presentation.acesso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planejadorfinanceiro.domain.model.ModoAcesso
import com.example.planejadorfinanceiro.domain.repository.ModoAcessoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ModoAcessoViewModel(
    private val repository: ModoAcessoRepository
) : ViewModel() {

    val modoAcesso: StateFlow<ModoAcesso> =
        repository.observarModoAcesso()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ModoAcesso.NAO_CONFIGURADO
            )

    fun salvarModoAcesso(modo: ModoAcesso) {
        viewModelScope.launch {
            repository.salvarModoAcesso(modo)
        }
    }
}