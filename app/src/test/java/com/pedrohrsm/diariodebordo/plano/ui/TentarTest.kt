package com.pedrohrsm.diariodebordo.plano.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TentarTest {

    @Test
    fun devolveOValorOuAFalha() = runTest {
        assertEquals(7, tentar { 7 }.getOrNull())
        val falha = tentar { error("disco cheio") }.exceptionOrNull()
        assertTrue(falha is IllegalStateException)
        assertEquals("disco cheio", falha?.message)
    }

    @Test
    fun cancelamentoSobeEnaoViraFalhaDeGravacao() = runTest {
        try {
            tentar { throw CancellationException("tela saiu") }
            fail("engoliu o cancelamento")
        } catch (e: CancellationException) {
            assertEquals("tela saiu", e.message)
        }
    }
}
