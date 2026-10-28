package com.pedrohrsm.diariodebordo.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contraste WCAG 2.x por par do `ColorScheme` (risco #40): texto >= 4,5:1 e borda >= 3:1, nos 3 temas, com e sem
 * modo oficina. Um token novo abaixo do piso reprova aqui, com o nome do par na mensagem. Esta lista é à mão (texto e bordas que o app
 * desenha com papéis do ColorScheme); as cores que os componentes do Material usam por padrão ficam no
 * [ContrasteComponentesTest], que as lê dos `*Defaults` (erro #55).
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
    )

    private fun contraste(a: Color, b: Color): Double = ContrasteWcag.contraste(a, b)

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
    fun contrasteSegueAFormulaWcag() {
        assertEquals(21.0, contraste(Color.Black, Color.White), 0.001)
        assertEquals(1.0, contraste(Color.White, Color.White), 0.001)
    }

    private companion object {
        const val MINIMO_TEXTO = ContrasteWcag.MINIMO_TEXTO
        const val MINIMO_BORDA = ContrasteWcag.MINIMO_COMPONENTE
    }
}
