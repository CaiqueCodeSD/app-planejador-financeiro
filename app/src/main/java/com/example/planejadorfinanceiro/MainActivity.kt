
package com.example.planejadorfinanceiro

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        configurarNavegacao()
    }

    private fun configurarNavegacao() {
        findViewById<View>(R.id.navHome).setOnClickListener {
            mostrarTela(contas = false)
        }

        findViewById<View>(R.id.navBills).setOnClickListener {
            mostrarTela(contas = true)
        }
    }

    private fun mostrarTela(contas: Boolean) {
        findViewById<View>(R.id.dashboardScreen).visibility =
            if (contas) View.GONE else View.VISIBLE

        findViewById<View>(R.id.billsScreen).visibility =
            if (contas) View.VISIBLE else View.GONE

        findViewById<View>(R.id.navHome)
            .setBackgroundResource(
                if (contas) android.R.color.transparent
                else R.drawable.pf_bg_nav_selected
            )

        findViewById<View>(R.id.navBills)
            .setBackgroundResource(
                if (contas) R.drawable.pf_bg_nav_selected
                else android.R.color.transparent
            )

        findViewById<TextView>(R.id.navHomeLabel)
            .setTextColor(
                getColor(
                    if (contas) R.color.pf_text_secondary
                    else R.color.pf_background
                )
            )

        findViewById<TextView>(R.id.navBillsLabel)
            .setTextColor(
                getColor(
                    if (contas) R.color.pf_background
                    else R.color.pf_text_secondary
                )
            )
    }
}
