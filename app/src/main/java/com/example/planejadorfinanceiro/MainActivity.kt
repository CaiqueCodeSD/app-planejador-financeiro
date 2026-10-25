package com.example.planejadorfinanceiro

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.planejadorfinanceiro.domain.model.Conta
import com.example.planejadorfinanceiro.domain.model.SituacaoConta
import com.example.planejadorfinanceiro.presentation.conta.ContaViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import android.content.res.ColorStateList
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val contaViewModel: ContaViewModel by viewModel()
    private var contaEmEdicaoId: Long? = null
    private var listaVaziaView: TextView? = null
    private var vencimentoSelecionadoMillis: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        listaVaziaView = findViewById(R.id.tvListaVazia)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        configurarNavegacao()
        configurarFormulario()
        observarEstadoCadastro()
        observarContas()
        configurarBotaoVoltar()
    }

    // --------------------------------------------------
    // NAVEGAÇÃO
    // --------------------------------------------------

    private fun configurarNavegacao() {
        val navHome = findViewById<View>(R.id.navHome)
        val navBills = findViewById<View>(R.id.navBills)

        val dashboardScreen = findViewById<View>(R.id.dashboardScreen)
        val billsScreen = findViewById<View>(R.id.billsScreen)
        val registerBillScreen = findViewById<View>(R.id.registerBillScreen)

        val navHomeLabel = findViewById<TextView>(R.id.navHomeLabel)
        val navBillsLabel = findViewById<TextView>(R.id.navBillsLabel)

        fun selecionarTela(contasSelecionadas: Boolean) {
            dashboardScreen.visibility =
                if (contasSelecionadas) View.GONE else View.VISIBLE

            billsScreen.visibility =
                if (contasSelecionadas) View.VISIBLE else View.GONE

            registerBillScreen.visibility = View.GONE

            navHome.setBackgroundResource(
                if (contasSelecionadas) {
                    android.R.color.transparent
                } else {
                    R.drawable.pf_bg_nav_selected
                }
            )

            navBills.setBackgroundResource(
                if (contasSelecionadas) {
                    R.drawable.pf_bg_nav_selected
                } else {
                    android.R.color.transparent
                }
            )

            navHomeLabel.setTextColor(
                getColor(
                    if (contasSelecionadas) {
                        R.color.pf_text_secondary
                    } else {
                        R.color.pf_background
                    }
                )
            )

            navBillsLabel.setTextColor(
                getColor(
                    if (contasSelecionadas) {
                        R.color.pf_background
                    } else {
                        R.color.pf_text_secondary
                    }
                )
            )
        }

        navHome.setOnClickListener {
            selecionarTela(contasSelecionadas = false)
        }

        navBills.setOnClickListener {
            selecionarTela(contasSelecionadas = true)
        }
    }

    private fun mostrarTela(telaId: Int) {
        findViewById<View>(R.id.dashboardScreen).visibility =
            if (telaId == R.id.dashboardScreen) View.VISIBLE else View.GONE

        findViewById<View>(R.id.billsScreen).visibility =
            if (telaId == R.id.billsScreen) View.VISIBLE else View.GONE

        findViewById<View>(R.id.registerBillScreen).visibility =
            if (telaId == R.id.registerBillScreen) View.VISIBLE else View.GONE
    }


    private fun atualizarNavegacao(isHome: Boolean) {
        val navHome = findViewById<View>(R.id.navHome)
        val navBills = findViewById<View>(R.id.navBills)

        val navHomeLabel = findViewById<TextView>(R.id.navHomeLabel)
        val navBillsLabel = findViewById<TextView>(R.id.navBillsLabel)

        navHome.setBackgroundResource(
            if (isHome) {
                R.drawable.pf_bg_nav_selected
            } else {
                android.R.color.transparent
            }
        )

        navBills.setBackgroundResource(
            if (isHome) {
                android.R.color.transparent
            } else {
                R.drawable.pf_bg_nav_selected
            }
        )

        navHomeLabel.setTextColor(
            getColor(
                if (isHome) R.color.pf_background
                else R.color.pf_text_secondary
            )
        )

        navBillsLabel.setTextColor(
            getColor(
                if (isHome) R.color.pf_text_secondary
                else R.color.pf_background
            )
        )
    }


    // --------------------------------------------------
    // FORMULÁRIO DE CADASTRO
    // --------------------------------------------------


    private fun configurarBotaoVoltar() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val telaCadastro = findViewById<View>(
                        R.id.registerBillScreen
                    )

                    val telaContas = findViewById<View>(
                        R.id.billsScreen
                    )

                    when {
                        telaCadastro.visibility == View.VISIBLE -> {
                            limparFormulario()
                            mostrarTela(R.id.billsScreen)
                            atualizarNavegacao(isHome = false)
                        }

                        telaContas.visibility == View.VISIBLE -> {
                            mostrarTela(R.id.dashboardScreen)
                            atualizarNavegacao(isHome = true)
                        }


                        else -> {
                            val dialog = AlertDialog.Builder(this@MainActivity)
                                .setTitle("Sair do aplicativo?")
                                .setMessage(
                                    "Deseja sair do Planejador Financeiro?"
                                )
                                .setNegativeButton("Continuar usando", null)
                                .setPositiveButton("Sair", null)
                                .create()

                            dialog.setOnShowListener {
                                val verde = getColor(R.color.pf_lime)

                                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setTextColor(verde)

                                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                    .setTextColor(verde)

                                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setOnClickListener {
                                        dialog.dismiss()
                                        finish()
                                    }

                                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                    .setOnClickListener {
                                        dialog.dismiss()
                                    }
                            }

                            dialog.show()
                        }

                    }
                }
            }
        )
    }


    private fun configurarFormulario() {
        findViewById<View>(R.id.btnNovaConta).setOnClickListener {
            abrirCadastro()
        }

        findViewById<TextView>(R.id.tvVencimentoConta).setOnClickListener {
            abrirSeletorData()
        }

        findViewById<Button>(R.id.btnSalvarConta).setOnClickListener {
            salvarConta()
        }

        findViewById<Button>(R.id.btnCancelarCadastroConta).setOnClickListener {
            limparFormulario()
            mostrarTela(R.id.billsScreen)
            atualizarNavegacao(isHome = false)
        }
    }



    private fun abrirCadastro() {
        limparFormulario()
        mostrarTela(R.id.registerBillScreen)
    }


    private fun abrirSeletorData() {
        val calendario = Calendar.getInstance()

        vencimentoSelecionadoMillis?.let {
            calendario.timeInMillis = it
        }

        DatePickerDialog(
            this,
            { _, ano, mes, dia ->
                calendario.set(Calendar.YEAR, ano)
                calendario.set(Calendar.MONTH, mes)
                calendario.set(Calendar.DAY_OF_MONTH, dia)
                calendario.set(Calendar.HOUR_OF_DAY, 12)
                calendario.set(Calendar.MINUTE, 0)
                calendario.set(Calendar.SECOND, 0)
                calendario.set(Calendar.MILLISECOND, 0)

                vencimentoSelecionadoMillis = calendario.timeInMillis

                val formato = SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale("pt", "BR")
                )

                findViewById<TextView>(R.id.tvVencimentoConta).text =
                    formato.format(calendario.time)

                findViewById<TextView>(R.id.tvErroCadastroConta).apply {
                    text = ""
                    visibility = View.GONE
                }
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }


    private fun salvarConta() {
        val descricao = findViewById<EditText>(R.id.etDescricaoConta)
            .text
            .toString()

        val valor = findViewById<EditText>(R.id.etValorConta)
            .text
            .toString()

        val vencimento = vencimentoSelecionadoMillis

        if (vencimento == null) {
            Toast.makeText(
                this,
                "Selecione a data de vencimento.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        contaViewModel.salvarConta(
            descricao = descricao,
            valorEmReais = valor,
            vencimentoMillis = vencimento,
            contaId = contaEmEdicaoId
        )
    }



    private fun observarEstadoCadastro() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                contaViewModel.uiState.collect { estado ->
                    val campoErro = findViewById<TextView>(
                        R.id.tvErroCadastroConta
                    )

                    campoErro.text = estado.mensagemErro.orEmpty()
                    campoErro.visibility =
                        if (estado.mensagemErro != null) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }


                    findViewById<Button>(R.id.btnSalvarConta).apply {
                        isEnabled = !estado.salvando

                        text = when {
                            estado.salvando && contaEmEdicaoId != null ->
                                "Atualizando..."

                            estado.salvando ->
                                "Salvando..."

                            contaEmEdicaoId != null ->
                                "Atualizar conta"

                            else ->
                                "Salvar conta"
                        }
                    }


                    if (estado.cadastroConcluido) {
                        val mensagem = if (contaEmEdicaoId != null) {
                            "Conta atualizada com sucesso!"
                        } else {
                            "Conta salva com sucesso!"
                        }

                        Toast.makeText(
                            this@MainActivity,
                            mensagem,
                            Toast.LENGTH_SHORT
                        ).show()

                        limparFormulario()
                        contaViewModel.limparFeedback()

                        mostrarTela(R.id.billsScreen)
                        atualizarNavegacao(isHome = false)
                    }
                }
            }
        }
    }


    private fun limparFormulario() {
        findViewById<EditText>(R.id.etDescricaoConta).text.clear()
        findViewById<EditText>(R.id.etValorConta).text.clear()

        vencimentoSelecionadoMillis = null

        findViewById<TextView>(R.id.tvVencimentoConta).text =
            "Selecionar data"

        findViewById<TextView>(R.id.tvErroCadastroConta).apply {
            text = ""
            visibility = View.GONE
        }

        contaEmEdicaoId = null

        findViewById<Button>(R.id.btnSalvarConta).text =
            "Salvar conta"
    }


    private fun exibirErro(mensagem: String) {
        Toast.makeText(
            this,
            mensagem,
            Toast.LENGTH_LONG
        ).show()
    }

    // --------------------------------------------------
    // LISTA REATIVA DE CONTAS
    // --------------------------------------------------

    private fun observarContas() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                contaViewModel.contas.collect { contas ->
                    atualizarTelaContas(contas)
                }
            }
        }
    }

    private fun atualizarTelaContas(contas: List<Conta>) {
        val pendentes = contas.filter {
            it.situacao == SituacaoConta.PENDENTE
        }

        val pagas = contas.filter {
            it.situacao == SituacaoConta.PAGA
        }

        val totalPendenteCentavos = pendentes.sumOf {
            it.valorCentavos
        }

        findViewById<TextView>(R.id.tvTotalPendente).text =
            formatarMoeda(totalPendenteCentavos)

        findViewById<TextView>(R.id.tvResumoPendentes).text =
            if (pendentes.size == 1) {
                "1 conta aguardando pagamento"
            } else {
                "${pendentes.size} contas aguardando pagamento"
            }

        findViewById<TextView>(R.id.tvQuantidadePendentes).text =
            "${pendentes.size} contas"

        findViewById<TextView>(R.id.tvQuantidadePagas).text =
            "${pagas.size} contas"

        val container = findViewById<LinearLayout>(R.id.containerListaContas)
        val listaVazia = listaVaziaView ?: return

        listaVazia.visibility = View.GONE

        // O container é reconstruído a cada atualização do Flow.
        container.removeAllViews()

        if (contas.isEmpty()) {
            listaVazia.visibility = View.VISIBLE
            container.addView(listaVazia)
            return
        }

        listaVazia.visibility = View.GONE

        contas.forEachIndexed { index, conta ->
            val item = layoutInflater.inflate(
                R.layout.item_conta,
                container,
                false
            )

            val calendario = Calendar.getInstance().apply {
                timeInMillis = conta.vencimentoMillis
            }

            item.findViewById<TextView>(R.id.tvDiaConta).text =
                calendario.get(Calendar.DAY_OF_MONTH).toString()

            val mes = SimpleDateFormat(
                "MMM",
                Locale("pt", "BR")
            ).format(Date(conta.vencimentoMillis))
                .uppercase(Locale("pt", "BR"))
                .replace(".", "")

            item.findViewById<TextView>(R.id.tvMesConta).text = mes

            item.findViewById<TextView>(R.id.tvDescricaoItemConta).text =
                conta.descricao

            val formatoData = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale("pt", "BR")
            )

            item.findViewById<TextView>(R.id.tvVencimentoItemConta).text =
                "Vencimento: ${formatoData.format(Date(conta.vencimentoMillis))}"

            val textoSituacao = when (conta.situacao) {
                SituacaoConta.PENDENTE -> "Pendente"
                SituacaoConta.PAGA -> "Paga"
            }

            val corSituacao = when (conta.situacao) {
                SituacaoConta.PENDENTE -> getColor(R.color.pf_expense)
                SituacaoConta.PAGA -> getColor(R.color.pf_lime)
            }

            item.findViewById<TextView>(R.id.tvSituacaoItemConta).apply {
                text = textoSituacao
                setTextColor(corSituacao)
            }

            item.findViewById<TextView>(R.id.tvValorItemConta).text =
                formatarMoeda(conta.valorCentavos)

            val btnMarcarComoPaga =
                item.findViewById<Button>(R.id.btnMarcarComoPaga)

            btnMarcarComoPaga.visibility =
                if (conta.situacao == SituacaoConta.PENDENTE) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            btnMarcarComoPaga.setOnClickListener {
                contaViewModel.marcarComoPaga(conta.id)
            }

            val btnEditarConta =
                item.findViewById<Button>(R.id.btnEditarConta)

            btnEditarConta.visibility =
                if (conta.situacao == SituacaoConta.PENDENTE) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            btnEditarConta.setOnClickListener {
                abrirEdicao(conta)
            }

            container.addView(item)

            if (index < contas.lastIndex) {
                val divisor = View(this).apply {
                    setBackgroundColor(getColor(R.color.pf_divider))
                }

                val alturaDivisor = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    1f,
                    resources.displayMetrics
                ).toInt().coerceAtLeast(1)

                container.addView(
                    divisor,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        alturaDivisor
                    )
                )
            }
        }
    }

    private fun formatarMoeda(valorCentavos: Long): String {
        return NumberFormat
            .getCurrencyInstance(Locale("pt", "BR"))
            .format(valorCentavos / 100.0)
    }

    private fun abrirEdicao(conta: Conta) {
        contaEmEdicaoId = conta.id

        findViewById<EditText>(R.id.etDescricaoConta)
            .setText(conta.descricao)

        findViewById<EditText>(R.id.etValorConta)
            .setText(
                java.math.BigDecimal.valueOf(
                    conta.valorCentavos,
                    2
                ).toPlainString()
            )

        vencimentoSelecionadoMillis = conta.vencimentoMillis

        findViewById<TextView>(R.id.tvVencimentoConta).text =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale("pt", "BR")
            ).format(Date(conta.vencimentoMillis))

        findViewById<TextView>(R.id.tvErroCadastroConta).apply {
            text = ""
            visibility = View.GONE
        }

        findViewById<Button>(R.id.btnSalvarConta).text =
            "Atualizar conta"

        mostrarTela(R.id.registerBillScreen)
    }
}
