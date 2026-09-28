package com.pedrohrsm.diariodebordo.core.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumTest {

    @Test
    fun comAFlagDesligadaNenhumRecursoPremiumFicaVisivel() {
        val gratis = Premium(habilitado = false)
        assertTrue(gratis.recursosVisiveis().isEmpty())
        RecursoPremium.entries.forEach { assertFalse(gratis.disponivel(it)) }
    }

    @Test
    fun gratisGuardaUmaMoto() {
        val gratis = Premium(habilitado = false)
        assertEquals(1, gratis.limiteDeMotos)
        assertTrue(gratis.podeCadastrarMoto(motosAtivas = 0))
        assertFalse(gratis.podeCadastrarMoto(motosAtivas = 1))
    }

    @Test
    fun premiumLigadoLiberaTudo() {
        val premium = Premium(habilitado = true)
        assertEquals(RecursoPremium.entries.toSet(), premium.recursosVisiveis())
        assertEquals(null, premium.limiteDeMotos)
        assertTrue(premium.podeCadastrarMoto(motosAtivas = 5))
    }
}
