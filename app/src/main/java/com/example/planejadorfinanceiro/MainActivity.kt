package com.example.planejadorfinanceiro

import androidx.activity.result.contract.ActivityResultContracts
import java.math.BigDecimal
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.planejadorfinanceiro.domain.model.Conta
import com.example.planejadorfinanceiro.domain.model.Movimentacao
import com.example.planejadorfinanceiro.domain.model.SituacaoConta
import com.example.planejadorfinanceiro.domain.model.TipoMovimentacao
import com.example.planejadorfinanceiro.presentation.conta.ContaViewModel
import com.example.planejadorfinanceiro.presentation.dashboard.DashboardViewModel
import com.example.planejadorfinanceiro.presentation.movimentacao.MovimentacaoViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import android.content.Intent
import com.example.planejadorfinanceiro.presentation.movimentacao.HistoricoMovimentacoesActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import android.widget.ImageButton

class MainActivity : AppCompatActivity() {

    private val contaViewModel: ContaViewModel by viewModel()
    private val dashboardViewModel: DashboardViewModel by viewModel()
    private val movimentacaoViewModel: MovimentacaoViewModel by viewModel()

    private var contaEmEdicaoId: Long? = null
    private var listaVaziaView: TextView? = null
    private var vencimentoSelecionadoMillis: Long? = null
    private var movimentacaoSelecionadaMillis: Long? = null

    private var movimentacaoEmEdicaoId: Long? = null

    private val historicoLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { resultado ->

            if (resultado.resultCode == RESULT_OK) {
                val id = resultado.data?.getLongExtra(
                    HistoricoMovimentacoesActivity.EXTRA_MOVIMENTACAO_ID,
                    -1L
                ) ?: -1L

                if (id > 0L) {
                    carregarMovimentacaoParaEdicao(id)
                }
            }
        }

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
        observarDashboard()
        configurarFormularioMovimentacao()
        observarEstadoMovimentacao()
        configurarBotaoVoltar()
        observarEstadoExclusao()
    }

    // --------------------------------------------------
    // NAVEGAÇÃO
    // --------------------------------------------------

    private fun configurarNavegacao() {
        findViewById<View>(R.id.navHome).setOnClickListener {
            limparFormularioMovimentacao()
            mostrarTela(R.id.dashboardScreen)
            atualizarNavegacao(isHome = true)
        }

        findViewById<View>(R.id.navBills).setOnClickListener {
            limparFormularioMovimentacao()
            mostrarTela(R.id.billsScreen)
            atualizarNavegacao(isHome = false)
        }

        mostrarTela(R.id.dashboardScreen)
        atualizarNavegacao(isHome = true)
    }

    private fun mostrarTela(telaId: Int) {
        findViewById<View>(R.id.dashboardScreen).visibility =
            if (telaId == R.id.dashboardScreen) View.VISIBLE else View.GONE

        findViewById<View>(R.id.billsScreen).visibility =
            if (telaId == R.id.billsScreen) View.VISIBLE else View.GONE

        findViewById<View>(R.id.registerBillScreen).visibility =
            if (telaId == R.id.registerBillScreen) View.VISIBLE else View.GONE

        findViewById<View>(R.id.registerMovementScreen).visibility =
            if (telaId == R.id.registerMovementScreen) View.VISIBLE else View.GONE
    }

    private fun atualizarNavegacao(isHome: Boolean) {
        val navHome = findViewById<View>(R.id.navHome)
        val navBills = findViewById<View>(R.id.navBills)
        val navHomeLabel = findViewById<TextView>(R.id.navHomeLabel)
        val navBillsLabel = findViewById<TextView>(R.id.navBillsLabel)

        navHome.setBackgroundResource(
            if (isHome) R.drawable.pf_bg_nav_selected
            else android.R.color.transparent
        )

        navBills.setBackgroundResource(
            if (isHome) android.R.color.transparent
            else R.drawable.pf_bg_nav_selected
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
    // BOTÃO VOLTAR
    // --------------------------------------------------

    private fun configurarBotaoVoltar() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        findViewById<View>(R.id.registerMovementScreen)
                            .visibility == View.VISIBLE -> {
                            limparFormularioMovimentacao()
                            mostrarTela(R.id.dashboardScreen)
                            atualizarNavegacao(isHome = true)
                        }

                        findViewById<View>(R.id.registerBillScreen)
                            .visibility == View.VISIBLE -> {
                            limparFormulario()
                            mostrarTela(R.id.billsScreen)
                            atualizarNavegacao(isHome = false)
                        }

                        findViewById<View>(R.id.billsScreen)
                            .visibility == View.VISIBLE -> {
                            mostrarTela(R.id.dashboardScreen)
                            atualizarNavegacao(isHome = true)
                        }

                        else -> confirmarSaida()
                    }
                }
            }
        )
    }

    private fun confirmarSaida() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Sair do aplicativo?")
            .setMessage("Deseja sair do Planejador Financeiro?")
            .setNegativeButton("Continuar usando", null)
            .setPositiveButton("Sair", null)
            .create()

        dialog.setOnShowListener {
            val verde = getColor(R.color.pf_lime)

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                setTextColor(verde)
                setOnClickListener {
                    dialog.dismiss()
                    finish()
                }
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
                setTextColor(verde)
                setOnClickListener { dialog.dismiss() }
            }
        }

        dialog.show()
    }

    // --------------------------------------------------
    // CADASTRO DE CONTAS
    // --------------------------------------------------

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
        atualizarNavegacao(isHome = false)
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

                findViewById<TextView>(R.id.tvVencimentoConta).text =
                    SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale("pt", "BR")
                    ).format(calendario.time)

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
            .text.toString()

        val valor = findViewById<EditText>(R.id.etValorConta)
            .text.toString()

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
                    val campoErro =
                        findViewById<TextView>(R.id.tvErroCadastroConta)

                    campoErro.text = estado.mensagemErro.orEmpty()
                    campoErro.visibility =
                        if (estado.mensagemErro != null) View.VISIBLE
                        else View.GONE

                    findViewById<Button>(R.id.btnSalvarConta).apply {
                        isEnabled = !estado.salvando
                        text = when {
                            estado.salvando && contaEmEdicaoId != null ->
                                "Atualizando..."
                            estado.salvando -> "Salvando..."
                            contaEmEdicaoId != null -> "Atualizar conta"
                            else -> "Salvar conta"
                        }
                    }

                    if (estado.cadastroConcluido) {
                        Toast.makeText(
                            this@MainActivity,
                            if (contaEmEdicaoId != null) {
                                "Conta atualizada com sucesso!"
                            } else {
                                "Conta salva com sucesso!"
                            },
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
        findViewById<Button>(R.id.btnSalvarConta).text = "Salvar conta"
    }

    // --------------------------------------------------
    // LISTA DE CONTAS
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

        findViewById<TextView>(R.id.tvTotalPendente).text =
            formatarMoeda(pendentes.sumOf { it.valorCentavos })

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

            item.findViewById<TextView>(R.id.tvMesConta).text =
                SimpleDateFormat("MMM", Locale("pt", "BR"))
                    .format(Date(conta.vencimentoMillis))
                    .uppercase(Locale("pt", "BR"))
                    .replace(".", "")

            item.findViewById<TextView>(R.id.tvDescricaoItemConta).text =
                conta.descricao

            item.findViewById<TextView>(R.id.tvVencimentoItemConta).text =
                "Vencimento: " + SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale("pt", "BR")
                ).format(Date(conta.vencimentoMillis))

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

            item.findViewById<Button>(R.id.btnMarcarComoPaga).apply {
                visibility =
                    if (conta.situacao == SituacaoConta.PENDENTE) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                setOnClickListener {
                    contaViewModel.marcarComoPaga(conta.id)
                }
            }

            item.findViewById<Button>(R.id.btnEditarConta).apply {
                visibility =
                    if (conta.situacao == SituacaoConta.PENDENTE) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                setOnClickListener {
                    abrirEdicao(conta)
                }
            }


            item.findViewById<ImageButton>(R.id.btnExcluirConta).apply {
                visibility = if (conta.situacao == SituacaoConta.PENDENTE) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                setOnClickListener {
                    confirmarExclusaoConta(conta)
                }
            }


            container.addView(item)

            if (index < contas.lastIndex) {
                val divisor = View(this).apply {
                    setBackgroundColor(getColor(R.color.pf_divider))
                }

                val altura = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    1f,
                    resources.displayMetrics
                ).toInt().coerceAtLeast(1)

                container.addView(
                    divisor,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        altura
                    )
                )
            }
        }
    }

    private fun abrirEdicao(conta: Conta) {
        contaEmEdicaoId = conta.id

        findViewById<EditText>(R.id.etDescricaoConta)
            .setText(conta.descricao)

        findViewById<EditText>(R.id.etValorConta).setText(
            java.math.BigDecimal.valueOf(conta.valorCentavos, 2).toPlainString()
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

        findViewById<Button>(R.id.btnSalvarConta).text = "Atualizar conta"

        mostrarTela(R.id.registerBillScreen)
        atualizarNavegacao(isHome = false)
    }

    // --------------------------------------------------
    // DASHBOARD
    // --------------------------------------------------

    private fun observarDashboard() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dashboardViewModel.uiState.collect { estado ->
                    findViewById<TextView>(R.id.tvSaldo).text =
                        formatarMoeda(estado.saldoCentavos)

                    findViewById<TextView>(R.id.tvReceitas).text =
                        formatarMoeda(estado.receitasCentavos)

                    findViewById<TextView>(R.id.tvDespesas).text =
                        formatarMoeda(estado.despesasCentavos)

                    atualizarMovimentacoesRecentes(
                        estado.movimentacoesRecentes
                    )
                }
            }
        }
    }

    private fun atualizarMovimentacoesRecentes(
        movimentacoes: List<Movimentacao>
    ) {
        val container = findViewById<LinearLayout>(
            R.id.containerMovimentacoesRecentes
        )

        val textoVazio = findViewById<TextView>(
            R.id.tvMovimentacoesVazias
        )

        container.removeAllViews()

        if (movimentacoes.isEmpty()) {
            textoVazio.visibility = View.VISIBLE
            return
        }

        textoVazio.visibility = View.GONE

        movimentacoes.forEachIndexed { index, movimentacao ->
            val item = layoutInflater.inflate(
                R.layout.item_movimentacao_recente,
                container,
                false
            )

            val isReceita = movimentacao.tipo == TipoMovimentacao.RECEITA

            item.findViewById<TextView>(R.id.tvIconeMovimentacao).apply {
                text = if (isReceita) "↑" else "↓"
                setTextColor(
                    getColor(
                        if (isReceita) R.color.pf_lime
                        else R.color.pf_expense
                    )
                )
            }

            item.findViewById<TextView>(R.id.tvDescricaoMovimentacao).text =
                movimentacao.descricao

            val dataFormatada = SimpleDateFormat(
                "dd MMM",
                Locale("pt", "BR")
            ).format(Date(movimentacao.dataMillis))

            item.findViewById<TextView>(R.id.tvDataMovimentacao).text =
                "${if (isReceita) "Receita" else "Despesa"} · $dataFormatada"

            item.findViewById<TextView>(R.id.tvValorMovimentacao).apply {
                text = (if (isReceita) "+ " else "- ") +
                        formatarMoeda(movimentacao.valorCentavos)

                setTextColor(
                    getColor(
                        if (isReceita) R.color.pf_lime
                        else R.color.pf_expense
                    )
                )
            }

            container.addView(item)

            if (index < movimentacoes.lastIndex) {
                val divisor = View(this).apply {
                    setBackgroundColor(getColor(R.color.pf_divider))
                }

                val altura = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    1f,
                    resources.displayMetrics
                ).toInt().coerceAtLeast(1)

                container.addView(
                    divisor,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        altura
                    )
                )
            }
        }
    }

    // --------------------------------------------------
    // CADASTRO DE MOVIMENTAÇÕES
    // --------------------------------------------------

    private fun configurarFormularioMovimentacao() {
        val categorias = listOf(
            "Selecione uma categoria",
            "Salário",
            "Freelance",
            "Investimentos",
            "Moradia",
            "Alimentação",
            "Transporte",
            "Saúde",
            "Educação",
            "Lazer",
            "Outros"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categorias
        ).apply {
            setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        findViewById<Spinner>(R.id.spCategoriaMovimentacao).adapter = adapter

        findViewById<View>(R.id.btnNovaMovimentacao).setOnClickListener {
            abrirCadastroMovimentacao()
        }

        findViewById<View>(R.id.tvDataMovimentacaoCadastro).setOnClickListener {
            selecionarDataMovimentacao()
        }

        findViewById<Button>(R.id.btnSalvarMovimentacao).setOnClickListener {
            salvarMovimentacao()
        }

        findViewById<Button>(R.id.btnCancelarMovimentacao).setOnClickListener {
            limparFormularioMovimentacao()
            mostrarTela(R.id.dashboardScreen)
            atualizarNavegacao(isHome = true)
        }

        findViewById<View>(R.id.btnVerHistorico).setOnClickListener {
            historicoLauncher.launch(
                Intent(
                    this,
                    HistoricoMovimentacoesActivity::class.java
                )
            )
        }

    }

    private fun carregarMovimentacaoParaEdicao(id: Long) {
        lifecycleScope.launch {
            val movimentacao =
                movimentacaoViewModel.buscarMovimentacaoPorId(id)

            if (movimentacao == null) {
                Toast.makeText(
                    this@MainActivity,
                    "Movimentação não encontrada.",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            movimentacaoEmEdicaoId = movimentacao.id

            findViewById<EditText>(R.id.etDescricaoMovimentacao)
                .setText(movimentacao.descricao)

            findViewById<EditText>(R.id.etValorMovimentacao)
                .setText(
                    BigDecimal.valueOf(
                        movimentacao.valorCentavos,
                        2
                    ).toPlainString()
                )

            findViewById<RadioGroup>(R.id.rgTipoMovimentacao).check(
                if (movimentacao.tipo == TipoMovimentacao.RECEITA) {
                    R.id.rbReceita
                } else {
                    R.id.rbDespesa
                }
            )

            val spinner = findViewById<Spinner>(
                R.id.spCategoriaMovimentacao
            )

            val posicaoCategoria = (0 until spinner.count).firstOrNull {
                spinner.getItemAtPosition(it).toString() == movimentacao.categoria
            } ?: 0

            spinner.setSelection(posicaoCategoria)

            spinner.setSelection(
                if (posicaoCategoria >= 0) posicaoCategoria else 0
            )

            movimentacaoSelecionadaMillis = movimentacao.dataMillis
            atualizarDataMovimentacao()

            findViewById<TextView>(R.id.tvErroMovimentacao).apply {
                text = ""
                visibility = View.GONE
            }

            findViewById<Button>(R.id.btnSalvarMovimentacao).text =
                "Atualizar movimentação"

            mostrarTela(R.id.registerMovementScreen)
            atualizarNavegacao(isHome = true)
        }
    }

    private fun abrirCadastroMovimentacao() {
        limparFormularioMovimentacao()

        val calendario = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        movimentacaoSelecionadaMillis = calendario.timeInMillis
        atualizarDataMovimentacao()

        mostrarTela(R.id.registerMovementScreen)
        atualizarNavegacao(isHome = true)
    }

    private fun selecionarDataMovimentacao() {
        val calendario = Calendar.getInstance().apply {
            timeInMillis = movimentacaoSelecionadaMillis
                ?: System.currentTimeMillis()
        }

        DatePickerDialog(
            this,
            { _, ano, mes, dia ->
                movimentacaoSelecionadaMillis = Calendar.getInstance().apply {
                    set(Calendar.YEAR, ano)
                    set(Calendar.MONTH, mes)
                    set(Calendar.DAY_OF_MONTH, dia)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                atualizarDataMovimentacao()
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun atualizarDataMovimentacao() {
        val data = movimentacaoSelecionadaMillis ?: return

        findViewById<TextView>(R.id.tvDataMovimentacaoCadastro).text =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale("pt", "BR")
            ).format(Date(data))
    }

    private fun salvarMovimentacao() {
        val descricao = findViewById<EditText>(
            R.id.etDescricaoMovimentacao
        ).text.toString().trim()

        val valor = findViewById<EditText>(
            R.id.etValorMovimentacao
        ).text.toString().trim()

        val tipo = when (
            findViewById<RadioGroup>(R.id.rgTipoMovimentacao)
                .checkedRadioButtonId
        ) {
            R.id.rbReceita -> TipoMovimentacao.RECEITA
            R.id.rbDespesa -> TipoMovimentacao.DESPESA
            else -> null
        }

        val spinner = findViewById<Spinner>(R.id.spCategoriaMovimentacao)
        val categoria = if (spinner.selectedItemPosition > 0) {
            spinner.selectedItem.toString()
        } else {
            ""
        }

        val data = movimentacaoSelecionadaMillis

        if (data == null) {
            exibirErroMovimentacao("Selecione a data da movimentação.")
            return
        }

        if (tipo == null) {
            exibirErroMovimentacao("Selecione receita ou despesa.")
            return
        }

        if (categoria.isBlank()) {
            exibirErroMovimentacao("Selecione uma categoria.")
            return
        }

        findViewById<TextView>(R.id.tvErroMovimentacao).visibility = View.GONE

        movimentacaoViewModel.salvarMovimentacao(
            descricao = descricao,
            valorEmReais = valor,
            tipo = tipo,
            dataMillis = data,
            categoria = categoria,
            movimentacaoId = movimentacaoEmEdicaoId
        )
    }


    private fun confirmarExclusaoConta(conta: Conta) {
        AlertDialog.Builder(this)
            .setTitle("Excluir conta?")
            .setMessage(
                "Deseja realmente excluir a conta \"${conta.descricao}\"?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ ->
                contaViewModel.excluirConta(conta.id)
            }
            .show()
    }


    private fun observarEstadoExclusao() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                contaViewModel.exclusaoUiState.collect { estado ->

                    if (estado.mensagemSucesso != null) {
                        Toast.makeText(
                            this@MainActivity,
                            estado.mensagemSucesso,
                            Toast.LENGTH_SHORT
                        ).show()

                        contaViewModel.limparFeedbackExclusao()
                    }

                    if (estado.mensagemErro != null) {
                        Toast.makeText(
                            this@MainActivity,
                            estado.mensagemErro,
                            Toast.LENGTH_LONG
                        ).show()

                        contaViewModel.limparFeedbackExclusao()
                    }
                }
            }
        }
    }



    private fun observarEstadoMovimentacao() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                movimentacaoViewModel.uiState.collect { estado ->

                    findViewById<Button>(R.id.btnSalvarMovimentacao).apply {
                        isEnabled = !estado.salvando

                        text = when {
                            estado.salvando && movimentacaoEmEdicaoId != null ->
                                "Atualizando..."

                            estado.salvando ->
                                "Salvando..."

                            movimentacaoEmEdicaoId != null ->
                                "Atualizar movimentação"

                            else ->
                                "Salvar movimentação"
                        }
                    }

                    findViewById<TextView>(R.id.tvErroMovimentacao).apply {
                        text = estado.mensagemErro.orEmpty()
                        visibility =
                            if (estado.mensagemErro != null) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                    }

                    if (estado.operacaoConcluida) {
                        Toast.makeText(
                            this@MainActivity,
                            estado.mensagemSucesso
                                ?: "Movimentação salva com sucesso.",
                            Toast.LENGTH_SHORT
                        ).show()

                        limparFormularioMovimentacao()
                        mostrarTela(R.id.dashboardScreen)
                        atualizarNavegacao(isHome = true)
                    }
                }
            }
        }
    }

    private fun exibirErroMovimentacao(mensagem: String) {
        findViewById<TextView>(R.id.tvErroMovimentacao).apply {
            text = mensagem
            visibility = View.VISIBLE
        }
    }

    private fun limparFormularioMovimentacao() {
        findViewById<EditText>(R.id.etDescricaoMovimentacao).text.clear()
        findViewById<EditText>(R.id.etValorMovimentacao).text.clear()

        findViewById<RadioGroup>(R.id.rgTipoMovimentacao)
            .check(R.id.rbReceita)

        findViewById<Spinner>(R.id.spCategoriaMovimentacao)
            .setSelection(0)

        findViewById<TextView>(R.id.tvErroMovimentacao).apply {
            text = ""
            visibility = View.GONE
        }

        movimentacaoEmEdicaoId = null
        movimentacaoSelecionadaMillis = null

        findViewById<TextView>(R.id.tvDataMovimentacaoCadastro).text =
            "Selecionar data"

        findViewById<Button>(R.id.btnSalvarMovimentacao).apply {
            text = "Salvar movimentação"
            isEnabled = true
        }

        movimentacaoViewModel.limparFeedback()
    }

    // --------------------------------------------------
    // UTILITÁRIO
    // --------------------------------------------------

    private fun formatarMoeda(valorCentavos: Long): String {
        return NumberFormat
            .getCurrencyInstance(Locale("pt", "BR"))
            .format(valorCentavos / 100.0)
    }
}
