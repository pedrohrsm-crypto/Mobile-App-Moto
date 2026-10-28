package com.pedrohrsm.diariodebordo.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.pedrohrsm.diariodebordo.navegacao.corRotuloSelecionadoBarra
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contraste WCAG 2.x por par do `ColorScheme` (risco #40): texto >= 4,5:1 e borda >= 3:1, nos 3 temas, com e sem
 * modo oficina. Um token novo abaixo do piso reprova aqui, com o nome do par na mensagem.
 */
class ContrasteTemaTest {
    private class Par(
        val nome: String,
        val primeiro: ColorScheme.() -> Color,
        val segundo: ColorScheme.() -> Color,
        val minimo: Double,
    )

    private fun texto(nome: String, frente: ColorScheme.() -> Color, fundo: ColorScheme.() -> Color) =
        Par(nome, frente, fundo, MINIMO_TEXTO)

    private fun borda(nome: String, linha: ColorScheme.() -> Color, fundo: ColorScheme.() -> Color) =
        Par(nome, linha, fundo, MINIMO_BORDA)

    private val pares = listOf(
        texto("onBackground/background", { onBackground }, { background }),
        texto("onSurface/surface", { onSurface }, { surface }),
        texto("onSurfaceVariant/background", { onSurfaceVariant }, { background }),
        texto("onSurfaceVariant/surface", { onSurfaceVariant }, { surface }),
        texto("onSurfaceVariant/surfaceVariant", { onSurfaceVariant }, { surfaceVariant }),
        texto("onPrimary/primary", { onPrimary }, { primary }),
        texto("onSecondary/secondary", { onSecondary }, { secondary }),
        texto("onTertiary/tertiary", { onTertiary }, { tertiary }),
        texto("onError/error", { onError }, { error }),
        texto("error/background", { error }, { background }),
        texto("error/surface", { error }, { surface }),
        texto("onPrimaryContainer/primaryContainer", { onPrimaryContainer }, { primaryContainer }),
        texto("onSecondaryContainer/secondaryContainer", { onSecondaryContainer }, { secondaryContainer }),
        texto("onTertiaryContainer/tertiaryContainer", { onTertiaryContainer }, { tertiaryContainer }),
        texto("onErrorContainer/errorContainer", { onErrorContainer }, { errorContainer }),
        // Rótulo "Vence logo" do PlanoScreen. O item destacado pela notificação não troca de fundo (borda em
        // outline): sobre secondaryContainer o "Vencido" dava 4,08:1 no Cromo e 4,45:1 no Oficina (PR #50).
        texto("primary/surface", { primary }, { surface }),
        borda("outline/background", { outline }, { background }),
        borda("outline/surface", { outline }, { surface }),
        borda("primary/background", { primary }, { background }), // traço da rota (TracadoRota), PR #51
        // Pares que o Material usa por padrão e o app não define (erro #55). Barra: container surfaceContainer.
        texto("barra rótulo selecionado/surfaceContainer", { corRotuloSelecionadoBarra(this) }, { surfaceContainer }),
        texto("barra rótulo não selecionado/surfaceContainer", { onSurfaceVariant }, { surfaceContainer }),
        texto("barra ícone selecionado/secondaryContainer", { onSecondaryContainer }, { secondaryContainer }),
        // AlertDialog (7 usos): container surfaceContainerHigh; TextButton em primary.
        texto("diálogo título onSurface/surfaceContainerHigh", { onSurface }, { surfaceContainerHigh }),
        texto("diálogo texto onSurfaceVariant/surfaceContainerHigh", { onSurfaceVariant }, { surfaceContainerHigh }),
        texto("diálogo botão primary/surfaceContainerHigh", { primary }, { surfaceContainerHigh }),
    )

    private fun luminanciaRelativa(cor: Color): Double {
        fun canal(valor: Float): Double = valor.toDouble().let {
            if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * canal(cor.red) + 0.7152 * canal(cor.green) + 0.0722 * canal(cor.blue)
    }

    private fun contraste(a: Color, b: Color): Double {
        val (claro, escuro) = listOf(luminanciaRelativa(a), luminanciaRelativa(b)).sortedDescending()
        return (claro + 0.05) / (escuro + 0.05)
    }

    private fun falhas(tokens: TokensDiario): List<String> {
        val esquema = tokens.paraColorScheme()
        return pares.mapNotNull { par ->
            val razao = contraste(par.primeiro(esquema), par.segundo(esquema))
            if (razao >= par.minimo) null else "%s %.2f:1 (mínimo %.1f)".format(par.nome, razao, par.minimo)
        }
    }

    @Test
    fun todoParDoColorSchemeAtingeOContrasteMinimoEmCadaTema() {
        TemaDiario.entries.forEach { tema ->
            listOf(false, true).forEach { modoOficina ->
                val tokens = tema.tokens().let { if (modoOficina) it.paraModoOficina() else it }
                val abaixo = falhas(tokens)
                assertTrue("$tema (modo oficina=$modoOficina) abaixo do mínimo: $abaixo", abaixo.isEmpty())
            }
        }
    }

    private fun assertAcusa(tokens: TokensDiario, par: String) {
        val abaixo = falhas(tokens)
        assertTrue(abaixo.toString(), abaixo.any { it.startsWith(par) })
    }

    @Test
    fun valoresAntigosDoFigmaReprovamComONomeDoPar() {
        // Os 3 valores de antes de 27-Out-2026 (risco #40): a conta tem de acusá-los, senão o teste não protege nada.
        val estradaAntigoBorda = TemaDiario.ESTRADA.tokens() + (PapelCor.TEXTO_TERCIARIO to Color(0xFF8A9499))
        val estradaAntigoRealce = TemaDiario.ESTRADA.tokens() + (PapelCor.REALCE to Color(0xFFC4552D))
        val cromoAntigoErro = TemaDiario.CROMO.tokens() + (PapelCor.CRITICO to Color(0xFFE03A3A))
        assertAcusa(estradaAntigoBorda, "outline/background")
        assertAcusa(estradaAntigoRealce, "onSecondary/secondary")
        assertAcusa(cromoAntigoErro, "error/background")
    }

    @Test
    fun rotuloSelecionadoDaBarraPassaDoMinimoEPadraoAntigoNao() {
        // Erro #55: o padrão do NavigationBarItem (secondary sobre surfaceContainer) dava 3,75:1 no Estrada.
        val estrada = TemaDiario.ESTRADA.tokens().paraColorScheme()
        assertEquals(3.75, contraste(estrada.secondary, estrada.surfaceContainer), 0.01)
        TemaDiario.entries.forEach { tema ->
            listOf(false, true).forEach { modoOficina ->
                val esquema = tema.tokens().let { if (modoOficina) it.paraModoOficina() else it }.paraColorScheme()
                val razao = contraste(corRotuloSelecionadoBarra(esquema), esquema.surfaceContainer)
                val msg = "$tema (oficina=$modoOficina) rótulo selecionado %.2f:1".format(razao)
                assertTrue(msg, razao >= MINIMO_TEXTO)
            }
        }
    }

    @Test
    fun parPadraoDeOutroComponentePlantadoAbaixoDoMinimoReprova() {
        // Canário de classe (#55): TEXTO_SECUNDARIO rebaixado quebra o rótulo não selecionado da barra e o texto
        // do diálogo, pares padrão do Material que o app não define.
        val rebaixado = TemaDiario.ESTRADA.tokens() + (PapelCor.TEXTO_SECUNDARIO to Color(0xFF8A9499))
        assertAcusa(rebaixado, "barra rótulo não selecionado/surfaceContainer")
        assertAcusa(rebaixado, "diálogo texto onSurfaceVariant/surfaceContainerHigh")
    }

    @Test
    fun contrasteSegueAFormulaWcag() {
        assertEquals(21.0, contraste(Color.Black, Color.White), 0.001)
        assertEquals(1.0, contraste(Color.White, Color.White), 0.001)
    }

    private companion object {
        const val MINIMO_TEXTO = 4.5
        const val MINIMO_BORDA = 3.0
    }
}
