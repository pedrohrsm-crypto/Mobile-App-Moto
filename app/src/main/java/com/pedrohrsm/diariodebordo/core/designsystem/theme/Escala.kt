package com.pedrohrsm.diariodebordo.core.designsystem.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Alvo de toque mínimo no modo normal (plano, seção UX/UI). */
private val ALVO_BASE = 48.dp

/** Figma, modo oficina: "letras e alvos 30% maiores". */
const val FATOR_MODO_OFICINA = 1.3f

/** Escala de tipografia e de espaçamento do tema. O modo oficina é a mesma escala com [fator] maior. */
@Immutable
data class EscalaDiario(val fator: Float = 1f) {
    val alvoMinimo: Dp = ALVO_BASE * fator

    fun espaco(base: Dp): Dp = base * fator

    fun margem(base: Dp): PaddingValues = PaddingValues(espaco(base))
}

val LocalEscalaDiario = staticCompositionLocalOf { EscalaDiario() }

fun escalaDe(modoOficina: Boolean): EscalaDiario = EscalaDiario(if (modoOficina) FATOR_MODO_OFICINA else 1f)

private fun TextStyle.escalar(fator: Float) = copy(fontSize = fontSize * fator, lineHeight = lineHeight * fator)

/** Todos os estilos de texto do Material crescem juntos; o tamanho continua em sp. */
fun Typography.escalar(fator: Float): Typography = if (fator == 1f) {
    this
} else {
    copy(
        displayLarge = displayLarge.escalar(fator),
        displayMedium = displayMedium.escalar(fator),
        displaySmall = displaySmall.escalar(fator),
        headlineLarge = headlineLarge.escalar(fator),
        headlineMedium = headlineMedium.escalar(fator),
        headlineSmall = headlineSmall.escalar(fator),
        titleLarge = titleLarge.escalar(fator),
        titleMedium = titleMedium.escalar(fator),
        titleSmall = titleSmall.escalar(fator),
        bodyLarge = bodyLarge.escalar(fator),
        bodyMedium = bodyMedium.escalar(fator),
        bodySmall = bodySmall.escalar(fator),
        labelLarge = labelLarge.escalar(fator),
        labelMedium = labelMedium.escalar(fator),
        labelSmall = labelSmall.escalar(fator),
    )
}
