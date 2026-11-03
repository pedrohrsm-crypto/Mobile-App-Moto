package com.pedrohrsm.diariodebordo.garagem.dominio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmacaoDeExclusaoTest {
    @Test
    fun confirmaSoComOApelidoDigitado() {
        assertFalse(confirmaExclusao("", "Bandida"))
        assertFalse(confirmaExclusao("Band", "Bandida"))
        assertFalse(confirmaExclusao("Bandida2", "Bandida"))
        assertFalse(confirmaExclusao("Bandída", "Bandida"))
        assertTrue(confirmaExclusao("Bandida", "Bandida"))
        // O teclado do celular capitaliza e deixa espaço no fim: não conta.
        assertTrue(confirmaExclusao(" bandida ", "Bandida"))
    }

    @Test
    fun apelidoVazioNuncaConfirma() {
        assertFalse(confirmaExclusao("", ""))
        assertFalse(confirmaExclusao("  ", " "))
    }
}
