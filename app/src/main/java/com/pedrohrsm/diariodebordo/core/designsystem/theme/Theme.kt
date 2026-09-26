package com.pedrohrsm.diariodebordo.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Os 3 temas do Figma (Oficina, Estrada, Cromo). As cores abaixo são provisórias:
 * serão trocadas pelos tokens do Figma, sempre com o par completo nos 3 temas.
 */
enum class TemaDiario { OFICINA, ESTRADA, CROMO }

private val Oficina = darkColorScheme(
    primary = Color(0xFFFF8A00),
    onPrimary = Color(0xFF1F1F1F),
    background = Color(0xFF1F1F1F),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF2A2A2A),
    onSurface = Color(0xFFF2F2F2),
)

private val Estrada = lightColorScheme(
    primary = Color(0xFF2E5E4E),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F4EE),
    onBackground = Color(0xFF1C1B19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B19),
)

private val Cromo = lightColorScheme(
    primary = Color(0xFF3A4A5C),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFEEF1F4),
    onBackground = Color(0xFF14181C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14181C),
)

fun TemaDiario.colorScheme(): ColorScheme = when (this) {
    TemaDiario.OFICINA -> Oficina
    TemaDiario.ESTRADA -> Estrada
    TemaDiario.CROMO -> Cromo
}

@Composable
fun DiarioTheme(
    tema: TemaDiario = TemaDiario.OFICINA,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = tema.colorScheme(), content = content)
}
