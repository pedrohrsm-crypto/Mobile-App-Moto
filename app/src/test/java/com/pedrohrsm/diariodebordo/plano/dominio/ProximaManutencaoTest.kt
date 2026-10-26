package com.pedrohrsm.diariodebordo.plano.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProximaManutencaoTest {

    private val trocaDeOleo = Intervalo(km = 3_000, dias = 180)

    @Test
    fun faltamKmEDiasQuandoNadaVenceu() {
        val s = situacaoDaManutencao(trocaDeOleo, kmNaUltimaVez = 10_000, kmAtual = 11_000, diasDesdeUltimaVez = 30)
        assertEquals(2_000, s.kmRestantes)
        assertEquals(150, s.diasRestantes)
        assertFalse(s.vencida)
    }

    @Test
    fun venceQuandoAtingeOKm() {
        val s = situacaoDaManutencao(trocaDeOleo, kmNaUltimaVez = 10_000, kmAtual = 13_000, diasDesdeUltimaVez = 30)
        assertTrue(s.vencida)
    }

    @Test
    fun vencePeloTempoMesmoRodandoPouco() {
        val s = situacaoDaManutencao(trocaDeOleo, kmNaUltimaVez = 10_000, kmAtual = 10_200, diasDesdeUltimaVez = 200)
        assertEquals(-20, s.diasRestantes)
        assertTrue(s.vencida)
    }

    @Test
    fun intervaloSoPorKmIgnoraTempo() {
        val s = situacaoDaManutencao(Intervalo(km = 1_000, dias = null), 0, 500, 999)
        assertEquals(null, s.diasRestantes)
        assertFalse(s.vencida)
    }

    @Test(expected = IllegalArgumentException::class)
    fun hodometroQueVoltaEhRecusado() {
        situacaoDaManutencao(trocaDeOleo, kmNaUltimaVez = 10_000, kmAtual = 9_000, diasDesdeUltimaVez = 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun intervaloVazioEhRecusado() {
        Intervalo(km = null, dias = null)
    }
}
