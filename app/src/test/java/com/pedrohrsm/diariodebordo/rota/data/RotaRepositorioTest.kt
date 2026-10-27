package com.pedrohrsm.diariodebordo.rota.data

import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.diario.data.FakeFotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.FakeRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.RegistroRepositorio
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.localizacao.Ponto
import com.pedrohrsm.diariodebordo.rota.dominio.Polilinha
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RotaRepositorioTest {

    private val inicio = Instant.parse("2026-10-04T09:00:00Z")
    private val relogio = Clock.fixed(inicio, ZoneId.of("America/Sao_Paulo"))
    private val rotaDao = FakeRotaDao()
    private val pontoDao = FakePontoDeRotaDao()
    private val registroDao = FakeRegistroDao()
    private val transacao = TransacaoFalsa()
    private val registros = RegistroRepositorio(
        registroDao,
        FakeFotoRegistroDao(),
        HodometroRepositorio(FakeLeituraHodometroDao(), transacao, relogio),
        transacao,
        relogio,
    )
    private val repositorio = RotaRepositorio(rotaDao, pontoDao, registros, transacao, relogio)

    private fun ponto(segundos: Long, lat: Double, precisao: Float? = 5f) =
        Ponto(lat, -46.6, inicio.toEpochMilli() + segundos * 1000, precisao)

    @Test
    fun iniciarDuasVezesDevolveAMesmaGravacao() = runTest {
        val a = repositorio.iniciar("moto")
        val b = repositorio.iniciar("moto")
        assertEquals(a.id, b.id)
        assertEquals(1, rotaDao.linhas.value.size)
        assertEquals("America/Sao_Paulo", a.fusoHorario)
    }

    @Test
    fun finalizarApagaPontosBrutosECriaORegistroDoRole() = runTest {
        val rota = repositorio.iniciar("moto")
        repositorio.adicionarPonto(rota.id, ponto(0, -23.50))
        repositorio.adicionarPonto(rota.id, ponto(300, -23.51, precisao = 200f))
        repositorio.adicionarPonto(rota.id, ponto(600, -23.52))

        val salva = repositorio.finalizar(rota.id)

        assertTrue(pontoDao.linhas.value.isEmpty())
        assertFalse(salva.emGravacao)
        assertEquals(600L, salva.duracaoS)
        // Só os pontos precisos contam: 0,02 grau de latitude, cerca de 2,2 km.
        assertEquals(2_224.0, salva.distanciaM.toDouble(), 2.0)
        assertEquals(2, Polilinha.decodificar(salva.polilinha).size)
        assertNull(repositorio.emGravacao())
        val role = registroDao.daMoto("moto").single()
        assertEquals(TipoRegistro.ROLE, role.tipo)
        assertEquals(rota.id, role.rotaId)
        assertEquals(rota.iniciadaEm, role.dataEm)
    }

    @Test
    fun finalizarSemPontosSalvaRotaVazia() = runTest {
        val rota = repositorio.iniciar("moto")
        val salva = repositorio.finalizar(rota.id)
        assertEquals(0L, salva.distanciaM)
        assertEquals("", salva.polilinha)
    }

    @Test(expected = IllegalStateException::class)
    fun finalizarDuasVezesFalha() = runTest {
        val rota = repositorio.iniciar("moto")
        repositorio.finalizar(rota.id)
        repositorio.finalizar(rota.id)
    }

    @Test
    fun descartarApagaPontosESomeDaGravacao() = runTest {
        val rota = repositorio.iniciar("moto")
        repositorio.adicionarPonto(rota.id, ponto(0, -23.50))
        repositorio.descartar(rota.id)
        assertTrue(pontoDao.linhas.value.isEmpty())
        assertNull(repositorio.emGravacao())
        assertTrue(registroDao.linhas.value.isEmpty())
    }

    @Test(expected = IllegalStateException::class)
    fun excluirRotaInexistenteFalha() = runTest {
        repositorio.excluir("nao-existe")
    }
}
