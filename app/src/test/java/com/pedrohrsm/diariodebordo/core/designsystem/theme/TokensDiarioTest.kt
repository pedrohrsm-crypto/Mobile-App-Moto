package com.pedrohrsm.diariodebordo.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Properties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TokensDiarioTest {
    private fun figma(tema: TemaDiario): Map<String, String> {
        val recurso = "/figma/tokens-${tema.name.lowercase()}.properties"
        val arquivo = checkNotNull(javaClass.getResourceAsStream(recurso)) { "faltou $recurso" }
        val propriedades = Properties().apply { arquivo.use { load(it.reader(Charsets.UTF_8)) } }
        return propriedades.stringPropertyNames().associateWith { propriedades.getProperty(it) }
    }

    private fun hex(cor: Color) = "#%06x".format(cor.toArgb() and 0xFFFFFF)

    /** Todas as cores que o `ColorScheme` expõe, lidas dos getters públicos (48 no Material 3 1.4). */
    private fun coresDo(esquema: ColorScheme): Map<String, Color> = ColorScheme::class.java.methods
        .filter { it.name.startsWith("get") && it.parameterCount == 0 && it.returnType == java.lang.Long.TYPE }
        .associate { it.name.removePrefix("get").substringBefore('-') to Color((it.invoke(esquema) as Long).toULong()) }

    @Test
    fun cadaTemaTemTodosOsPapeisDeCor() {
        TemaDiario.entries.forEach { tema ->
            val faltam = PapelCor.entries.toSet() - tema.tokens().keys
            assertTrue("tema $tema sem par para $faltam", faltam.isEmpty())
        }
    }

    @Test
    fun cadaTokenBateComAVariavelDoFigma() {
        TemaDiario.entries.forEach { tema ->
            val doFigma = figma(tema)
            assertEquals(
                "variáveis do Figma no tema $tema não batem com os papéis do app",
                PapelCor.entries.map { it.variavelFigma }.toSet(),
                doFigma.keys,
            )
            PapelCor.entries.forEach { papel ->
                assertEquals(
                    "$tema / $papel",
                    doFigma.getValue(papel.variavelFigma),
                    hex(tema.tokens().getValue(papel)),
                )
            }
        }
    }

    @Test
    fun colorSchemeSoUsaCoresDosTokens() {
        TemaDiario.entries.forEach { tema ->
            listOf(false, true).forEach { modoOficina ->
                val tokens = tema.tokens().let { if (modoOficina) it.paraModoOficina() else it }
                val permitidas = tokens.values.toSet()
                val esquema = coresDo(tokens.paraColorScheme())
                assertEquals("o Material 3 deveria expor 48 papéis", 48, esquema.size)
                val fora = esquema.filterValues { it !in permitidas }.keys
                assertTrue(
                    "$tema (modo oficina=$modoOficina): papéis do Material fora dos tokens: $fora",
                    fora.isEmpty(),
                )
            }
        }
    }

    @Test
    fun modoOficinaDaContrasteTotalAoTextoSecundario() {
        TemaDiario.entries.forEach { tema ->
            val tokens = tema.tokens().paraModoOficina()
            assertEquals(tokens.getValue(PapelCor.TEXTO), tokens.getValue(PapelCor.TEXTO_SECUNDARIO))
            assertEquals(tokens.getValue(PapelCor.TEXTO), tokens.getValue(PapelCor.TEXTO_TERCIARIO))
            // o resto do tema não muda
            val intactos = PapelCor.entries - setOf(PapelCor.TEXTO_SECUNDARIO, PapelCor.TEXTO_TERCIARIO)
            intactos.forEach { assertEquals(tema.tokens().getValue(it), tokens.getValue(it)) }
        }
    }

    @Test
    fun oficinaECromoSaoEscurosEEstradaEClaro() {
        assertTrue(TemaDiario.OFICINA.tokens().escuro())
        assertTrue(TemaDiario.CROMO.tokens().escuro())
        assertFalse(TemaDiario.ESTRADA.tokens().escuro())
    }
}
