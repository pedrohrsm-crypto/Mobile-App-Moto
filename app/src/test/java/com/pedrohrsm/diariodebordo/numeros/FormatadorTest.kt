package com.pedrohrsm.diariodebordo.numeros

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatadorTest {

    @Test
    fun kmUsaPontoDeMilhar() {
        assertEquals("12.345 km", Formatador.km(12_345))
        assertEquals("0 km", Formatador.km(0))
    }

    @Test
    fun dinheiroEmReaisComDuasCasas() {
        assertEquals("R$ 1.234,56", Formatador.dinheiro(123_456))
        assertEquals("R$ 0,05", Formatador.dinheiro(5))
        assertEquals("-R$ 10,00", Formatador.dinheiro(-1_000))
    }

    @Test
    fun litrosComDuasCasas() {
        assertEquals("12,50 L", Formatador.litros(12_500))
        assertEquals("1.000,00 L", Formatador.litros(1_000_000))
    }

    @Test
    fun dataNoFusoDoRegistro() {
        val meiaNoiteUtc = 1_790_812_800_000 // 2026-10-01T00:00:00Z
        assertEquals("01/10/2026", Formatador.data(meiaNoiteUtc, ZoneId.of("UTC")))
        assertEquals("30/09/2026", Formatador.data(meiaNoiteUtc, ZoneId.of("America/Sao_Paulo")))
    }

    @Test
    fun fusoInvalidoCaiNoDoAparelho() {
        assertEquals(ZoneId.systemDefault(), Formatador.fuso(""))
        assertEquals(ZoneId.of("America/Manaus"), Formatador.fuso("America/Manaus"))
    }
}

class LeitorTest {

    @Test
    fun kmAceitaPontoDeMilharEEspacos() {
        assertEquals(12_345, Leitor.km("12.345"))
        assertEquals(12_345, Leitor.km(" 12345 km"))
        assertNull(Leitor.km("12,5"))
        assertNull(Leitor.km("-3"))
        assertNull(Leitor.km(""))
        assertNull(Leitor.km("12345678"))
    }

    @Test
    fun dinheiroEmCentavos() {
        assertEquals(123_456L, Leitor.dinheiro("1.234,56"))
        assertEquals(123_450L, Leitor.dinheiro("R$ 1234,5"))
        assertEquals(1_000L, Leitor.dinheiro("10"))
        assertNull(Leitor.dinheiro("1,234"))
        assertNull(Leitor.dinheiro("abc"))
    }

    @Test
    fun litrosEmMililitros() {
        assertEquals(12_500L, Leitor.litros("12,5"))
        assertEquals(9_000L, Leitor.litros("9 L"))
        assertNull(Leitor.litros("1,2345"))
    }
}

class TextoDeNumeroTest {

    @Test
    fun textoDeEdicaoVoltaAoMesmoValorPeloLeitor() {
        listOf(0L, 5L, 1_000L, 123_456L).forEach {
            assertEquals(it, Leitor.dinheiro(TextoDeNumero.dinheiro(it)))
        }
        listOf(0L, 500L, 9_000L, 12_345L).forEach {
            assertEquals(it, Leitor.litros(TextoDeNumero.litros(it)))
        }
        assertEquals("12,5", TextoDeNumero.litros(12_500))
    }
}
