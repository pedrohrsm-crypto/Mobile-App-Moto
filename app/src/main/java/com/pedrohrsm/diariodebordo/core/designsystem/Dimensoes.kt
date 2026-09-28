package com.pedrohrsm.diariodebordo.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Escala de espaçamento e alvo de toque. O modo oficina troca por uma escala maior. */
data class Dimensoes(val toqueMinimo: Dp = 48.dp, val espaco: Dp = 16.dp, val espacoPequeno: Dp = 8.dp) {
    companion object {
        /** Alvo de toque para dedo com luva: bem acima dos 48 dp do Material. */
        val OFICINA = Dimensoes(toqueMinimo = 64.dp, espaco = 24.dp, espacoPequeno = 12.dp)
    }
}

val LocalDimensoes = staticCompositionLocalOf { Dimensoes() }
