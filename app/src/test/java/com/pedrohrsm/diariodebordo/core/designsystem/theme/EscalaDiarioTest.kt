package com.pedrohrsm.diariodebordo.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class EscalaDiarioTest {
    @Test
    fun modoNormalMantemAlvoDe48dp() {
        assertEquals(48.dp, escalaDe(modoOficina = false).alvoMinimo)
    }

    @Test
    fun modoOficinaAumenta30PorCentoOAlvoEOEspaco() {
        val escala = escalaDe(modoOficina = true)
        assertEquals(62.4f, escala.alvoMinimo.value, 0.001f)
        assertEquals(20.8f, escala.espaco(16.dp).value, 0.001f)
    }

    @Test
    fun modoOficinaAumentaTodosOsEstilosDeTextoEmSp() {
        val base = Typography()
        val grande = base.escalar(FATOR_MODO_OFICINA)
        listOf(
            base.displayLarge to grande.displayLarge,
            base.titleLarge to grande.titleLarge,
            base.bodyLarge to grande.bodyLarge,
            base.bodyMedium to grande.bodyMedium,
            base.labelSmall to grande.labelSmall,
        ).forEach { (antes, depois) ->
            assertEquals(antes.fontSize.value * FATOR_MODO_OFICINA, depois.fontSize.value, 0.001f)
            assertEquals(antes.lineHeight.value * FATOR_MODO_OFICINA, depois.lineHeight.value, 0.001f)
            assertEquals(antes.fontSize.type, depois.fontSize.type)
        }
    }

    @Test
    fun fatorUmNaoMexeNaTipografia() {
        val base = Typography()
        assertSame(base, base.escalar(1f))
    }
}
