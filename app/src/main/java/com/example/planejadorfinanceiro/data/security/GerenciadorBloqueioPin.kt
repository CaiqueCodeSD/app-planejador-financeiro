package com.example.planejadorfinanceiro.data.security

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.planejadorfinanceiro.domain.model.EstadoBloqueioPin
import com.example.planejadorfinanceiro.domain.repository.PinRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GerenciadorBloqueioPin(
    private val pinRepository: PinRepository
) : DefaultLifecycleObserver {

    companion object {
        private const val TEMPO_BLOQUEIO = 5 * 60 * 1000L
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private val _estado = MutableStateFlow(EstadoBloqueioPin())

    val estado: StateFlow<EstadoBloqueioPin> = _estado.asStateFlow()

    private var horarioSegundoPlano: Long? = null
    private var iniciado = false

    fun iniciar() {
        if (iniciado) return
        iniciado = true

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        scope.launch {
            pinRepository.observarPinAtivado().collect { ativado ->
                _estado.update { atual ->
                    atual.copy(
                        pinAtivado = ativado,
                        desbloqueado = if (ativado) {
                            atual.desbloqueado
                        } else {
                            false
                        },
                        inicializado = true
                    )
                }
            }
        }
    }

    fun desbloquear() {
        _estado.update {
            it.copy(desbloqueado = true)
        }
    }

    fun bloquear() {
        _estado.update {
            it.copy(desbloqueado = false)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        horarioSegundoPlano = SystemClock.elapsedRealtime()
    }

    override fun onStart(owner: LifecycleOwner) {
        val horarioSaida = horarioSegundoPlano ?: return

        val tempoDecorrido =
            SystemClock.elapsedRealtime() - horarioSaida

        if (tempoDecorrido >= TEMPO_BLOQUEIO) {
            bloquear()
        }

        horarioSegundoPlano = null
    }
}
