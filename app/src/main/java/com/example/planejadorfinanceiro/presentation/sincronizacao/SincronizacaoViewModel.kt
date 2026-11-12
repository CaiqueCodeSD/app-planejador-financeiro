package com.example.planejadorfinanceiro.presentation.sincronizacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planejadorfinanceiro.domain.model.EstadoSincronizacao
import com.example.planejadorfinanceiro.domain.model.ModoAcesso
import com.example.planejadorfinanceiro.domain.repository.ModoAcessoRepository
import com.example.planejadorfinanceiro.domain.repository.SincronizacaoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SincronizacaoViewModel(
    private val repository: SincronizacaoRepository,
    private val modoAcessoRepository: ModoAcessoRepository
) : ViewModel() {

    val uiState: StateFlow<SincronizacaoUiState> =
        combine(
            repository.estado,
            modoAcessoRepository.observarModoAcesso()
        ) { estado, modoAcesso ->

            if (modoAcesso != ModoAcesso.CONECTADO_GOOGLE) {
                return@combine SincronizacaoUiState.Oculta
            }

            when (estado) {
                EstadoSincronizacao.Ociosa ->
                    SincronizacaoUiState.Aguardando

                EstadoSincronizacao.EmAndamento ->
                    SincronizacaoUiState.EmAndamento

                is EstadoSincronizacao.Concluida ->
                    SincronizacaoUiState.Concluida(
                        estado.totalSincronizado
                    )

                is EstadoSincronizacao.Erro ->
                    SincronizacaoUiState.Erro(estado.mensagem)

                EstadoSincronizacao.AutorizacaoNecessaria ->
                    SincronizacaoUiState.AutorizacaoNecessaria
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SincronizacaoUiState.Oculta
        )

    fun sincronizar() {
        viewModelScope.launch {
            repository.sincronizar()
        }
    }
}