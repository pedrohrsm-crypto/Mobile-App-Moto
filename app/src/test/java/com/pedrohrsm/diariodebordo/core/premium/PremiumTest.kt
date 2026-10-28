package com.pedrohrsm.diariodebordo.core.premium

import com.pedrohrsm.diariodebordo.BuildConfig
import com.pedrohrsm.diariodebordo.garagem.dominio.LIMITE_MOTOS_GRATIS
import com.pedrohrsm.diariodebordo.garagem.dominio.podeCadastrarMoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumTest {

    @Test
    fun flagDesligadaNaoLiberaNenhumRecurso() {
        val gratis = Premium(habilitado = false)
        assertEquals(emptySet<RecursoPremium>(), gratis.recursosVisiveis())
        RecursoPremium.entries.forEach { assertFalse(it.name, gratis.disponivel(it)) }
        assertEquals(LIMITE_MOTOS_GRATIS, gratis.limiteDeMotos)
        assertFalse(podeCadastrarMoto(1, gratis.limiteDeMotos))
    }

    @Test
    fun flagLigadaMostraTudoETiraOLimiteDeMotos() {
        val pago = Premium(habilitado = true)
        assertEquals(RecursoPremium.entries.toSet(), pago.recursosVisiveis())
        assertNull(pago.limiteDeMotos)
        assertTrue(podeCadastrarMoto(5, pago.limiteDeMotos))
    }

    @Test
    fun flagDoBuildVemDesligadaNoMvp() {
        assertFalse("o MVP não publica recurso pago (1.10)", BuildConfig.PREMIUM_HABILITADO)
    }
}
