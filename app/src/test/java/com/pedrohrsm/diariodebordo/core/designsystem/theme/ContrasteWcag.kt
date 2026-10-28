package com.pedrohrsm.diariodebordo.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/** Contraste WCAG 2.x entre duas cores opacas, compartilhado pelos testes de contraste. */
internal object ContrasteWcag {
    const val MINIMO_TEXTO = 4.5
    const val MINIMO_COMPONENTE = 3.0

    private fun luminanciaRelativa(cor: Color): Double {
        fun canal(valor: Float): Double = valor.toDouble().let {
            if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * canal(cor.red) + 0.7152 * canal(cor.green) + 0.0722 * canal(cor.blue)
    }

    fun contraste(a: Color, b: Color): Double {
        val (claro, escuro) = listOf(luminanciaRelativa(a), luminanciaRelativa(b)).sortedDescending()
        return (claro + 0.05) / (escuro + 0.05)
    }
}
