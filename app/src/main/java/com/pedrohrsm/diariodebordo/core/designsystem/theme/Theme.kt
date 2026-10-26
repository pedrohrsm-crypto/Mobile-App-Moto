package com.pedrohrsm.diariodebordo.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Tokens do tema ativo, inclusive os papéis sem par no Material (OK, realce, navegação…). */
val LocalTokensDiario = staticCompositionLocalOf { TemaDiario.OFICINA.tokens() }

object DiarioTema {
    @Composable
    @ReadOnlyComposable
    fun cor(papel: PapelCor): Color = LocalTokensDiario.current.getValue(papel)
}

/**
 * Todo papel do `ColorScheme` sai de um token do tema; nada fica no padrão roxo do Material.
 * `TokensDiarioTest` confere que nenhuma cor do esquema escapa dos tokens.
 */
fun TokensDiario.paraColorScheme(): ColorScheme {
    fun cor(papel: PapelCor) = getValue(papel)
    return ColorScheme(
        primary = cor(PapelCor.DESTAQUE),
        onPrimary = cor(PapelCor.SOBRE_DESTAQUE),
        primaryContainer = cor(PapelCor.DESTAQUE_SUAVE),
        onPrimaryContainer = cor(PapelCor.TEXTO),
        inversePrimary = cor(PapelCor.REALCE),
        secondary = cor(PapelCor.REALCE),
        onSecondary = cor(PapelCor.SOBRE_DESTAQUE),
        secondaryContainer = cor(PapelCor.DESTAQUE_SUAVE),
        onSecondaryContainer = cor(PapelCor.TEXTO),
        tertiary = cor(PapelCor.OK),
        onTertiary = cor(PapelCor.SOBRE_DESTAQUE),
        tertiaryContainer = cor(PapelCor.OK_SUAVE),
        onTertiaryContainer = cor(PapelCor.TEXTO),
        background = cor(PapelCor.FUNDO),
        onBackground = cor(PapelCor.TEXTO),
        surface = cor(PapelCor.SUPERFICIE),
        onSurface = cor(PapelCor.TEXTO),
        surfaceVariant = cor(PapelCor.SUPERFICIE_ALTA),
        onSurfaceVariant = cor(PapelCor.TEXTO_SECUNDARIO),
        surfaceTint = cor(PapelCor.DESTAQUE),
        inverseSurface = cor(PapelCor.TEXTO),
        inverseOnSurface = cor(PapelCor.FUNDO),
        error = cor(PapelCor.CRITICO),
        onError = cor(PapelCor.SOBRE_DESTAQUE),
        errorContainer = cor(PapelCor.CRITICO_SUAVE),
        onErrorContainer = cor(PapelCor.TEXTO),
        outline = cor(PapelCor.TEXTO_TERCIARIO),
        outlineVariant = cor(PapelCor.LINHA),
        scrim = cor(PapelCor.VEU),
        surfaceBright = cor(PapelCor.SUPERFICIE_ALTA),
        surfaceDim = cor(PapelCor.FUNDO),
        surfaceContainer = cor(PapelCor.NAVEGACAO),
        surfaceContainerHigh = cor(PapelCor.SUPERFICIE_ALTA),
        surfaceContainerHighest = cor(PapelCor.SUPERFICIE_ALTA),
        surfaceContainerLow = cor(PapelCor.SUPERFICIE),
        surfaceContainerLowest = cor(PapelCor.FUNDO),
        primaryFixed = cor(PapelCor.DESTAQUE_SUAVE),
        primaryFixedDim = cor(PapelCor.DESTAQUE),
        onPrimaryFixed = cor(PapelCor.TEXTO),
        onPrimaryFixedVariant = cor(PapelCor.TEXTO_SECUNDARIO),
        secondaryFixed = cor(PapelCor.DESTAQUE_SUAVE),
        secondaryFixedDim = cor(PapelCor.REALCE),
        onSecondaryFixed = cor(PapelCor.TEXTO),
        onSecondaryFixedVariant = cor(PapelCor.TEXTO_SECUNDARIO),
        tertiaryFixed = cor(PapelCor.OK_SUAVE),
        tertiaryFixedDim = cor(PapelCor.OK),
        onTertiaryFixed = cor(PapelCor.TEXTO),
        onTertiaryFixedVariant = cor(PapelCor.TEXTO_SECUNDARIO),
    )
}

@Composable
fun DiarioTheme(tema: TemaDiario = TemaDiario.OFICINA, modoOficina: Boolean = false, content: @Composable () -> Unit) {
    val tokens = remember(tema, modoOficina) {
        tema.tokens().let { if (modoOficina) it.paraModoOficina() else it }
    }
    val escala = remember(modoOficina) { escalaDe(modoOficina) }
    CompositionLocalProvider(LocalTokensDiario provides tokens, LocalEscalaDiario provides escala) {
        MaterialTheme(
            colorScheme = remember(tokens) { tokens.paraColorScheme() },
            typography = remember(escala) { Typography().escalar(escala.fator) },
            content = content,
        )
    }
}
