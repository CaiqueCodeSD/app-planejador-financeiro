package com.example.planejadorfinanceiro.presentation.movimentacao

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.planejadorfinanceiro.R
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class HistoricoMovimentacoesActivity : AppCompatActivity() {

    private val movimentacaoViewModel: MovimentacaoViewModel by viewModel()

    private lateinit var adapter: MovimentacaoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico_movimentacoes)

        configurarLista()
        configurarBotaoVoltar()
        observarMovimentacoes()
        observarEstadoOperacoes()
    }

    private fun configurarLista() {
        adapter = MovimentacaoAdapter(
            onEditClick = { movimentacao ->
                val resultado = Intent().apply {
                    putExtra(EXTRA_MOVIMENTACAO_ID, movimentacao.id)
                }

                setResult(RESULT_OK, resultado)
                finish()
            },
            onDeleteClick = { movimentacao ->
                confirmarExclusao(movimentacao.id)
            }
        )

        findViewById<RecyclerView>(R.id.rvHistoricoMovimentacoes).apply {
            layoutManager = LinearLayoutManager(this@HistoricoMovimentacoesActivity)
            adapter = this@HistoricoMovimentacoesActivity.adapter
        }
    }

    private fun observarMovimentacoes() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                movimentacaoViewModel.movimentacoes.collect { movimentacoes ->
                    adapter.submitList(movimentacoes)

                    findViewById<TextView>(R.id.tvHistoricoVazio).visibility =
                        if (movimentacoes.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                }
            }
        }
    }

    private fun observarEstadoOperacoes() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                movimentacaoViewModel.uiState.collect { estado ->

                    when {
                        estado.operacaoConcluida -> {
                            estado.mensagemSucesso?.let { mensagem ->
                                Toast.makeText(
                                    this@HistoricoMovimentacoesActivity,
                                    mensagem,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            movimentacaoViewModel.limparFeedback()
                        }

                        estado.mensagemErro != null -> {
                            Toast.makeText(
                                this@HistoricoMovimentacoesActivity,
                                estado.mensagemErro,
                                Toast.LENGTH_LONG
                            ).show()

                            movimentacaoViewModel.limparFeedback()
                        }
                    }
                }
            }
        }
    }

    private fun confirmarExclusao(id: Long) {
        AlertDialog.Builder(this)
            .setTitle("Excluir movimentação?")
            .setMessage(
                "Essa movimentação será removida do histórico. Deseja continuar?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ ->
                movimentacaoViewModel.excluirMovimentacao(id)
            }
            .show()
    }

    private fun configurarBotaoVoltar() {
        findViewById<View>(R.id.btnVoltarHistorico).setOnClickListener {
            finish()
        }
    }

    companion object {
        const val EXTRA_MOVIMENTACAO_ID = "extra_movimentacao_id"
    }
}