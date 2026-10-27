package com.pedrohrsm.diariodebordo.garagem.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HodometroTest {

    @Test
    fun primeiraLeituraEhAceita() {
        assertEquals(ValidacaoLeitura.Aceita, validarLeitura(anteriorKm = null, novoKm = 0))
    }

    @Test
    fun hodometroNuncaAceitaValorMenorQueOUltimo() {
        assertEquals(ValidacaoLeitura.MenorQueAnterior(10_000), validarLeitura(10_000, 9_999))
    }

    @Test
    fun mesmoKmNaoViraLeituraNova() {
        assertEquals(ValidacaoLeitura.IgualAnterior(10_000), validarLeitura(10_000, 10_000))
    }

    @Test
    fun negativoEAbsurdoSaoRecusados() {
        assertEquals(ValidacaoLeitura.Negativa, validarLeitura(null, -1))
        assertEquals(ValidacaoLeitura.AcimaDoLimite, validarLeitura(null, KM_MAXIMO + 1))
    }

    @Test
    fun cadastroExigeCamposEKmValido() {
        val invalidos = validarMoto(" ", "Honda", "", "1850", "12,3", anoAtual = 2026)
        assertEquals(setOf(CampoMoto.APELIDO, CampoMoto.MODELO, CampoMoto.ANO, CampoMoto.KM), invalidos)
    }

    @Test
    fun edicaoNaoValidaKmEAnoEhOpcional() {
        assertTrue(validarMoto("Bandida", "Harley-Davidson", "883", "", kmTexto = null, anoAtual = 2026).isEmpty())
        assertTrue(validarMoto("Bandida", "Harley-Davidson", "883", "2027", kmTexto = "0", anoAtual = 2026).isEmpty())
    }

    @Test
    fun gratisPermiteUmaMotoEPremiumSemLimite() {
        assertTrue(podeCadastrarMoto(motosAtivas = 0))
        assertFalse(podeCadastrarMoto(motosAtivas = LIMITE_MOTOS_GRATIS))
        assertTrue(podeCadastrarMoto(motosAtivas = 5, limite = null))
    }
}
