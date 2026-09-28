package com.pedrohrsm.diariodebordo.plano.data

import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.diario.data.FakeFotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.FakeRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.RegistroRepositorio
import com.pedrohrsm.diariodebordo.diario.data.TipoOcorrencia
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.CategoriaMoto
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import com.pedrohrsm.diariodebordo.plano.dominio.planoBase
import com.pedrohrsm.diariodebordo.plano.ui.montarPlano
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanoRepositorioTest {

    private val instante = Instant.parse("2026-10-01T12:00:00Z")
    private val relogio = Clock.fixed(instante, ZoneId.of("America/Sao_Paulo"))
    private val dao = FakeItemDoPlanoDao()
    private val plano = PlanoRepositorio(dao, relogio)
    private val hodometro = HodometroRepositorio(FakeLeituraHodometroDao(), relogio)
    private val registros = RegistroRepositorio(
        FakeRegistroDao(),
        FakeFotoRegistroDao(),
        hodometro,
        plano,
        TransacaoFalsa(),
        relogio,
    )
    private val moto = Moto(id = "moto", apelido = "Bandida", marca = "Harley-Davidson", modelo = "883")

    private suspend fun oleo(): ItemDoPlano = plano.observarDaMoto("moto").first().single { it.nome == "Troca de óleo" }

    @Test
    fun cadastroSemeiaOPlanoBaseDaCategoriaContandoDoKmInicial() = runTest {
        plano.semearPlanoBase(moto.copy(categoria = CategoriaMoto.CUSTOM), kmInicial = 20_000)
        val itens = plano.observarDaMoto("moto").first()
        assertEquals(planoBase(CategoriaMoto.CUSTOM).size, itens.size)
        assertTrue(itens.all { it.ultimaVezKm == 20_000 && it.origem == OrigemItem.PLANO_BASE })
    }

    @Test
    fun manutencaoDoItemNoDiarioODaPorFeito() = runTest {
        plano.semearPlanoBase(moto, kmInicial = 10_000)
        plano.marcarAvisado(oleo().id, 1L)
        val registro = Registro(
            motoId = "moto",
            tipo = TipoRegistro.MANUTENCAO,
            titulo = "Troca de óleo",
            dataEm = instante.toEpochMilli(),
            hodometroKm = 14_900,
            itemPlanoId = oleo().id,
        )
        val salvo = registros.salvar(registro, emptyList()).registro
        val item = oleo()
        assertEquals(14_900, item.ultimaVezKm)
        assertEquals(salvo.id, item.ultimoRegistroId)
        assertNull(item.avisadoEm)
    }

    @Test
    fun registroMaisAntigoNaoVoltaAReferencia() = runTest {
        plano.semearPlanoBase(moto, kmInicial = 10_000)
        val id = oleo().id
        plano.registrarFeito(id, 15_000, quando = 2_000L, registroId = "novo")
        plano.registrarFeito(id, 12_000, quando = 1_000L, registroId = "velho")
        assertEquals(15_000, oleo().ultimaVezKm)
    }

    @Test
    fun ocorrenciaCriaPendenciasUmaVezESomemAoResolver() = runTest {
        val queda = Registro(
            motoId = "moto",
            tipo = TipoRegistro.OCORRENCIA,
            titulo = "Tombo parado",
            dataEm = instante.toEpochMilli(),
            ocorrencia = TipoOcorrencia.QUEDA,
        )
        val salvo = registros.salvar(queda, emptyList()).registro
        registros.salvar(salvo.copy(descricao = "Só o retrovisor"), emptyList())
        val pendencias = plano.observarDaMoto("moto").first()
        assertEquals(4, pendencias.size)
        assertTrue(pendencias.all { it.pendencia && it.registroOrigemId == salvo.id })
        pendencias.forEach { plano.resolverPendencia(it.id) }
        assertTrue(plano.observarDaMoto("moto").first().isEmpty())
    }

    @Test
    fun mudarIntervaloLimpaOAvisoEPlanoOrdenaPorUrgencia() = runTest {
        plano.semearPlanoBase(moto, kmInicial = 10_000)
        plano.marcarAvisado(oleo().id, 1L)
        plano.salvar(oleo().copy(intervaloKm = 800), kmAtual = 10_000)
        assertNull(oleo().avisadoEm)
        val tela = montarPlano(moto, plano.observarDaMoto("moto").first(), kmAtual = 11_200, relogio)
        assertEquals(Urgencia.VENCIDO, tela.itens.first().situacao.urgencia)
        assertEquals("Troca de óleo", tela.itens.first().item.nome)
    }
}
