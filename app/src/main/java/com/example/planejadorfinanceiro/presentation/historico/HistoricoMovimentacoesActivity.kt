
package com.example.planejadorfinanceiro.presentation.movimentacao

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
    }

    private fun configurarLista() {
        adapter = MovimentacaoAdapter(
            onItemClick = { movimentacao ->
                Toast.makeText(
                    this,
                    "Edição será integrada na próxima etapa.",
                    Toast.LENGTH_SHORT
                ).show()
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

                    findViewById<TextView>(
                        R.id.tvHistoricoVazio
                    ).visibility = if (movimentacoes.isEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
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

                Toast.makeText(
                    this,
                    "Solicitação de exclusão realizada.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun configurarBotaoVoltar() {
        findViewById<View>(R.id.btnVoltarHistorico).setOnClickListener {
            finish()
        }
    }
}
