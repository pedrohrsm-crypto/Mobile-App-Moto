package com.pedrohrsm.diariodebordo.garagem.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class MotoRepositorioTest {

    private val instante = Instant.parse("2026-10-01T12:00:00Z")
    private val relogio = Clock.fixed(instante, ZoneId.of("America/Sao_Paulo"))
    private val dao = FakeMotoDao()
    private val repositorio = MotoRepositorio(dao, relogio)

    private fun exemplo() = Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "Sportster 883", ano = 2012)

    @Test
    fun criarPreencheOsCamposDeControle() = runTest {
        val criado = repositorio.criar(exemplo())
        assertEquals(instante.toEpochMilli(), criado.criadoEm)
        assertEquals(instante.toEpochMilli(), criado.alteradoEm)
        assertEquals("America/Sao_Paulo", criado.fusoHorario)
        assertNull(criado.excluidoEm)
        assertFalse(criado.pendenteSync)
        assertEquals(listOf(criado), repositorio.observarAtivos().first())
    }

    @Test
    fun excluirEscondeDaListaSemApagarALinha() = runTest {
        val criado = repositorio.criar(exemplo())
        repositorio.excluir(criado.id)
        assertTrue(repositorio.observarAtivos().first().isEmpty())
        assertEquals(1, dao.linhas.value.size)
    }

    @Test(expected = IllegalStateException::class)
    fun atualizarMotoInexistenteFalhaEmVezDeIgnorar() = runTest {
        repositorio.atualizar(exemplo())
    }

    @Test(expected = IllegalStateException::class)
    fun excluirDuasVezesFalha() = runTest {
        val criado = repositorio.criar(exemplo())
        repositorio.excluir(criado.id)
        repositorio.excluir(criado.id)
    }
}
