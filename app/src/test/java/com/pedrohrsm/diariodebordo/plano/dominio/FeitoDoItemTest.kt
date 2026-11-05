package com.pedrohrsm.diariodebordo.plano.dominio

import com.pedrohrsm.diariodebordo.garagem.dominio.KM_MAXIMO
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A regra única do feito do item (#69), na JVM. */
class FeitoDoItemTest {

    private val hoje = LocalDate.of(2026, 11, 4)

    @Test
    fun itemFeitoNoKmAntigoAcompanhaACorrecao() {
        assertTrue(acompanhaCorrecaoDoHodometro(ultimaVezKm = 50_000, kmAntigo = 50_000))
    }

    @Test
    fun itemFeitoEmOutroKmNaoAcompanhaACorrecao() {
        assertFalse(acompanhaCorrecaoDoHodometro(ultimaVezKm = 49_999, kmAntigo = 50_000))
        assertFalse(acompanhaCorrecaoDoHodometro(ultimaVezKm = null, kmAntigo = 50_000))
    }

    @Test
    fun kmDoFeitoNosLimitesDeZeroAoKmAtualEAceito() {
        assertEquals(emptySet<CampoItem>(), validarFeito(0, hoje, kmAtual = 7_000, hoje = hoje))
        assertEquals(emptySet<CampoItem>(), validarFeito(7_000, hoje, kmAtual = 7_000, hoje = hoje))
    }

    @Test
    fun kmDoFeitoAcimaDoKmAtualERecusado() {
        assertEquals(setOf(CampoItem.FEITO_KM), validarFeito(7_001, hoje, kmAtual = 7_000, hoje = hoje))
    }

    @Test
    fun kmDoFeitoNegativoERecusado() {
        assertEquals(setOf(CampoItem.FEITO_KM), validarFeito(-1, hoje, kmAtual = 7_000, hoje = hoje))
    }

    @Test
    fun kmDoFeitoIlegivelERecusado() {
        assertEquals(setOf(CampoItem.FEITO_KM), validarFeito("5.0x0", hoje, kmAtual = 7_000, hoje = hoje))
    }

    @Test
    fun semLeituraOKmDoFeitoVaiAteOLimiteDoHodometro() {
        assertEquals(emptySet<CampoItem>(), validarFeito(KM_MAXIMO, hoje, kmAtual = null, hoje = hoje))
        assertEquals(setOf(CampoItem.FEITO_KM), validarFeito(KM_MAXIMO + 1, hoje, kmAtual = null, hoje = hoje))
    }

    @Test
    fun dataDoFeitoDepoisDeHojeERecusada() {
        assertEquals(setOf(CampoItem.FEITO_DATA), validarFeito(100, hoje.plusDays(1), kmAtual = 7_000, hoje = hoje))
    }
}
