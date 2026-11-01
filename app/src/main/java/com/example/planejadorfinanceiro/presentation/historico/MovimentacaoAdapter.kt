
package com.example.planejadorfinanceiro.presentation.movimentacao

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.planejadorfinanceiro.R
import com.example.planejadorfinanceiro.domain.model.Movimentacao
import com.example.planejadorfinanceiro.domain.model.TipoMovimentacao
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MovimentacaoAdapter(
    private val onEditClick: (Movimentacao) -> Unit,
    private val onDeleteClick: (Movimentacao) -> Unit
) : ListAdapter<Movimentacao, MovimentacaoAdapter.MovimentacaoViewHolder>(
    DiffCallback
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MovimentacaoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_movimentacao_historico,
                parent,
                false
            )

        return MovimentacaoViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MovimentacaoViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    inner class MovimentacaoViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val descricao: TextView =
            itemView.findViewById(R.id.tvDescricaoHistorico)

        private val categoria: TextView =
            itemView.findViewById(R.id.tvCategoriaHistorico)

        private val data: TextView =
            itemView.findViewById(R.id.tvDataHistorico)

        private val valor: TextView =
            itemView.findViewById(R.id.tvValorHistorico)

        private val botaoEditar: View =
            itemView.findViewById(R.id.btnEditarMovimentacao)

        private val botaoExcluir: View =
            itemView.findViewById(R.id.btnExcluirMovimentacao)

        fun bind(movimentacao: Movimentacao) {
            descricao.text = movimentacao.descricao
            categoria.text = movimentacao.categoria

            data.text = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale("pt", "BR")
            ).format(Date(movimentacao.dataMillis))

            val formatoMoeda = NumberFormat.getCurrencyInstance(
                Locale("pt", "BR")
            )

            val valorFormatado = formatoMoeda.format(
                movimentacao.valorCentavos / 100.0
            )

            val prefixo = if (
                movimentacao.tipo == TipoMovimentacao.RECEITA
            ) {
                "+ "
            } else {
                "- "
            }

            valor.text = prefixo + valorFormatado

            botaoEditar.setOnClickListener {
                onEditClick(movimentacao)
            }

            botaoExcluir.setOnClickListener {
                onDeleteClick(movimentacao)
            }
        }
    }

    companion object {
        private val DiffCallback =
            object : DiffUtil.ItemCallback<Movimentacao>() {

                override fun areItemsTheSame(
                    oldItem: Movimentacao,
                    newItem: Movimentacao
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: Movimentacao,
                    newItem: Movimentacao
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }
}
