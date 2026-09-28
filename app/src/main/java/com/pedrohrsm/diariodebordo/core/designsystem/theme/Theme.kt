package com.pedrohrsm.diariodebordo.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.isSpecified
import com.pedrohrsm.diariodebordo.core.designsystem.Dimensoes
import com.pedrohrsm.diariodebordo.core.designsystem.LocalDimensoes

/*
 * Cores dos 3 temas do Figma (Oficina, Estrada, Cromo). As cores abaixo são provisórias:
 * serão trocadas pelos tokens do Figma, sempre com o par completo nos 3 temas.
 */

private val coresOficina = darkColorScheme(
    primary = Color(0xFFFF8A00),
    onPrimary = Color(0xFF1F1F1F),
    background = Color(0xFF1F1F1F),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF2A2A2A),
    onSurface = Color(0xFFF2F2F2),
)

private val coresEstrada = lightColorScheme(
    primary = Color(0xFF2E5E4E),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F4EE),
    onBackground = Color(0xFF1C1B19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B19),
)

private val coresCromo = lightColorScheme(
    primary = Color(0xFF3A4A5C),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFEEF1F4),
    onBackground = Color(0xFF14181C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14181C),
)

fun TemaDiario.colorScheme(): ColorScheme = when (this) {
    TemaDiario.OFICINA -> coresOficina
    TemaDiario.ESTRADA -> coresEstrada
    TemaDiario.CROMO -> coresCromo
}

/**
 * Tema do app. O modo oficina não é um quarto tema: vale com qualquer um dos 3 e só aumenta texto,
 * espaçamento e alvos de toque, para usar com luva ou com a mão suja (plano de ação, 1.8).
 */
@Composable
fun DiarioTheme(tema: TemaDiario = TemaDiario.OFICINA, modoOficina: Boolean = false, content: @Composable () -> Unit) {
    val tipografia = if (modoOficina) Typography().ampliada(ESCALA_TEXTO_OFICINA) else Typography()
    CompositionLocalProvider(LocalDimensoes provides if (modoOficina) Dimensoes.OFICINA else Dimensoes()) {
        MaterialTheme(colorScheme = tema.colorScheme(), typography = tipografia, content = content)
    }
}

/** Soma-se à fonte do sistema: 140% no aparelho vira 175% no modo oficina. */
private const val ESCALA_TEXTO_OFICINA = 1.25f

private fun TextStyle.ampliado(fator: Float) = copy(
    fontSize = if (fontSize.isSpecified) fontSize * fator else fontSize,
    lineHeight = if (lineHeight.isSpecified) lineHeight * fator else lineHeight,
)

private fun Typography.ampliada(fator: Float) = Typography(
    displayLarge = displayLarge.ampliado(fator),
    displayMedium = displayMedium.ampliado(fator),
    displaySmall = displaySmall.ampliado(fator),
    headlineLarge = headlineLarge.ampliado(fator),
    headlineMedium = headlineMedium.ampliado(fator),
    headlineSmall = headlineSmall.ampliado(fator),
    titleLarge = titleLarge.ampliado(fator),
    titleMedium = titleMedium.ampliado(fator),
    titleSmall = titleSmall.ampliado(fator),
    bodyLarge = bodyLarge.ampliado(fator),
    bodyMedium = bodyMedium.ampliado(fator),
    bodySmall = bodySmall.ampliado(fator),
    labelLarge = labelLarge.ampliado(fator),
    labelMedium = labelMedium.ampliado(fator),
    labelSmall = labelSmall.ampliado(fator),
)
