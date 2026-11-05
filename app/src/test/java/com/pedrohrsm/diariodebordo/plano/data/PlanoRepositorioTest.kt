package com.pedrohrsm.diariodebordo.plano.data

import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.TipoOcorrencia
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.CategoriaMoto
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.plano.dominio.CHAVE_TROCA_DE_OLEO
import com.pedrohrsm.diariodebordo.plano.dominio.CampoItem
import com.pedrohrsm.diariodebordo.plano.dominio.FeitoRecusadoException
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import com.pedrohrsm.diariodebordo.plano.dominio.planoBase
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import com.pedrohrsm.diariodebordo.plano.ui.montarPlano
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PlanoRepositorioTest {

    private val instante = Instant.parse("2026-10-01T12:00:00Z")
    private val relogio = Clock.fixed(instante, ZoneId.of("America/Sao_Paulo"))
    private val dao = FakeItemDoPlanoDao()
    private val agendador = AgendadorFalso()
    private val plano = PlanoRepositorio(dao, TransacaoFalsa(), relogio, agendador)
    private val moto = Moto(id = "moto", apelido = "Bandida", marca = "Harley-Davidson", modelo = "883")

    private suspend fun itens(): List<ItemDoPlano> = plano.observarDaMoto("moto").first()

    private suspend fun oleo(): ItemDoPlano = itens().single { it.chave == CHAVE_TROCA_DE_OLEO }

    private fun ocorrencia(tipo: TipoOcorrencia?, motoId: String = "moto") = Registro(
        motoId = motoId,
        tipo = TipoRegistro.OCORRENCIA,
        titulo = "Ocorrência",
        dataEm = instante.toEpochMilli(),
        ocorrencia = tipo,
    )

    @Test
    fun primeiraAberturaCriaOPlanoBaseDaCategoriaContandoDoKmEDeAgora() = runTest {
        val criados = plano.garantirPlanoBase(moto.copy(categoria = CategoriaMoto.CUSTOM), kmAtual = 20_000)
        val esperados = planoBase(CategoriaMoto.CUSTOM).map { it.nome }.sorted()
        assertEquals(esperados.size, criados)
        val itens = itens()
        assertEquals(esperados, itens.map { it.nome }.sorted())
        assertTrue(itens.all { it.ultimaVezKm == 20_000 && it.ultimaVezEm == instante.toEpochMilli() })
        assertTrue(itens.all { it.origem == OrigemItem.PLANO_BASE && it.feitoEm == null })
        assertEquals(instante.toEpochMilli(), oleo().criadoEm)
    }

    @Test
    fun planoBaseVemUmaVezSoEQuemApagouNaoORecebeDeVolta() = runTest {
        plano.garantirPlanoBase(moto, kmAtual = 10_000)
        assertEquals(0, plano.garantirPlanoBase(moto, kmAtual = 10_000))
        itens().forEach { plano.excluir(it.id) }
        assertEquals(0, plano.garantirPlanoBase(moto, kmAtual = 10_000))
        assertTrue(itens().isEmpty())
    }

    @Test
    fun motoSemHodometroComecaDoZero() = runTest {
        plano.garantirPlanoBase(moto, kmAtual = null)
        assertTrue(itens().all { it.ultimaVezKm == 0 })
    }

    @Test
    fun marcarFeitoRecomecaAContaDoKmAtualELimpaOAviso() = runTest {
        plano.garantirPlanoBase(moto, kmAtual = 10_000)
        val antes = oleo()
        plano.marcarAvisado(antes.id, Urgencia.VENCIDO)
        plano.marcarFeito(antes.id, kmAtual = 14_900)
        val depois = oleo()
        assertEquals(14_900, depois.ultimaVezKm)
        assertEquals(instante.toEpochMilli(), depois.feitoEm)
        assertNull(depois.avisadoComo)
    }

    @Test
    fun marcarFeitoDeItemInexistenteFalhaEmVezDeSumir() = runTest {
        try {
            plano.marcarFeito("nao-existe", kmAtual = 1_000)
            fail("aceitou item inexistente")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("nao-existe"))
        }
    }

    @Test
    fun ocorrenciaCriaPendenciasUmaVezESomemAoResolver() = runTest {
        val queda = ocorrencia(TipoOcorrencia.QUEDA)
        dao.registros.value = listOf(queda, ocorrencia(TipoOcorrencia.BARULHO, motoId = "outra"))
        assertEquals(4, plano.sincronizarPendencias("moto"))
        // Rodar de novo (cada abertura do plano roda) não duplica.
        assertEquals(0, plano.sincronizarPendencias("moto"))
        val pendencias = itens()
        assertEquals(4, pendencias.size)
        assertTrue(pendencias.all { it.pendencia && it.registroOrigemId == queda.id && it.intervalo == null })
        pendencias.forEach { plano.resolverPendencia(it.id) }
        assertTrue(itens().isEmpty())
    }

    @Test
    fun mudarIntervaloLimpaOAvisoPedeRecalculoEPlanoOrdenaPorUrgencia() = runTest {
        plano.garantirPlanoBase(moto, kmAtual = 10_000)
        plano.marcarAvisado(oleo().id, Urgencia.PROXIMO)
        plano.salvar(oleo().copy(intervaloKm = 800), kmAtual = 10_000)
        assertNull(oleo().avisadoComo)
        assertEquals(1, agendador.recalculos)
        val tela = montarPlano(moto, itens(), kmAtual = 11_200, relogio)
        assertEquals(Urgencia.VENCIDO, tela.itens.first().situacao.urgencia)
        assertEquals(CHAVE_TROCA_DE_OLEO, tela.itens.first().item.chave)
    }

    @Test
    fun editarNaoMudaAChaveNemOBase() = runTest {
        plano.garantirPlanoBase(moto, kmAtual = 10_000)
        plano.salvar(oleo().copy(nome = "Óleo 10W40", chave = null, origem = OrigemItem.USUARIO), kmAtual = 10_000)
        val editado = oleo()
        assertEquals("Óleo 10W40", editado.nome)
        assertEquals(OrigemItem.PLANO_BASE, editado.origem)
    }

    @Test
    fun itemNovoDaPessoaContaDoKmAtualEPedeRecalculo() = runTest {
        val novo = plano.salvar(
            ItemDoPlano(motoId = "moto", nome = "Cabo do acelerador", intervaloDias = 365),
            kmAtual = 7_000,
        )
        assertEquals(7_000, novo.ultimaVezKm)
        assertEquals(instante.toEpochMilli(), novo.ultimaVezEm)
        assertEquals(OrigemItem.USUARIO, novo.origem)
        assertEquals(1, agendador.recalculos)
    }

    @Test
    fun marcarAvisadoGravaAUrgenciaEDizSeAchouOItem() = runTest {
        plano.garantirPlanoBase(moto, kmAtual = 10_000)
        val id = oleo().id
        assertTrue(plano.marcarAvisado(id, Urgencia.PROXIMO))
        assertEquals(Urgencia.PROXIMO, oleo().avisadoComo)
        plano.excluir(id)
        assertFalse(plano.marcarAvisado(id, Urgencia.VENCIDO))
    }

    @Test
    fun atualizacaoQueNaoAchaALinhaFalhaEmVezDeFingirQueGravou() = runTest {
        plano.garantirPlanoBase(moto, kmAtual = 10_000)
        val id = oleo().id
        dao.linhasDaAtualizacao = 0
        listOf<suspend () -> Unit>(
            { plano.marcarFeito(id, kmAtual = 11_000) },
            { plano.salvar(oleo().copy(nome = "Outro nome"), kmAtual = 11_000) },
            { plano.resolverPendencia(id) },
        ).forEach { escrita ->
            try {
                escrita()
                fail("fingiu que gravou")
            } catch (e: IllegalStateException) {
                assertTrue(e.message.orEmpty().contains("0 linhas"))
            }
        }
    }

    @Test
    fun excluirItemInexistenteFalhaEmVezDeSumir() = runTest {
        try {
            plano.excluir("nao-existe")
            fail("aceitou excluir item inexistente")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("0 linhas"))
        }
    }

    /** Óleo a cada 3.000 km, feito aos 5.000 em 1-Set, já avisado como próximo. */
    private suspend fun oleoFeito(): ItemDoPlano {
        val item = ItemDoPlano(
            id = "oleo",
            motoId = "moto",
            nome = "Óleo",
            intervaloKm = 3_000,
            ultimaVezKm = 5_000,
            ultimaVezEm = Instant.parse("2026-09-01T15:00:00Z").toEpochMilli(),
            feitoEm = Instant.parse("2026-09-01T15:00:00Z").toEpochMilli(),
            avisadoComo = Urgencia.PROXIMO,
        )
        dao.inserir(item)
        return item
    }

    @Test
    fun editarOFeitoGravaKmEDataELimpaOAviso() = runTest {
        val item = oleoFeito()
        val novaData = Instant.parse("2026-09-20T15:00:00Z").toEpochMilli()

        plano.salvar(item.copy(ultimaVezKm = 6_200, ultimaVezEm = novaData), kmAtual = 7_000)

        val salvo = dao.linhas.value.single()
        assertEquals(6_200, salvo.ultimaVezKm)
        assertEquals(novaData, salvo.ultimaVezEm)
        assertEquals(novaData, salvo.feitoEm)
        assertNull(salvo.avisadoComo)
        assertEquals(1, agendador.recalculos)
    }

    @Test
    fun feitoComKmAcimaDoAtualERecusadoSemGravar() = runTest {
        val item = oleoFeito()
        try {
            plano.salvar(item.copy(ultimaVezKm = 7_001), kmAtual = 7_000)
            fail("km do feito acima do km atual devia ser recusado")
        } catch (e: FeitoRecusadoException) {
            assertEquals(setOf(CampoItem.FEITO_KM), e.campos)
        }
        assertEquals(listOf(item), dao.linhas.value)
        assertEquals(0, agendador.recalculos)
    }

    @Test
    fun feitoComDataDepoisDeHojeERecusadoSemGravar() = runTest {
        val item = oleoFeito()
        val amanha = instante.plusSeconds(86_400).toEpochMilli()
        try {
            plano.salvar(item.copy(ultimaVezEm = amanha), kmAtual = 7_000)
            fail("data do feito depois de hoje devia ser recusada")
        } catch (e: FeitoRecusadoException) {
            assertEquals(setOf(CampoItem.FEITO_DATA), e.campos)
        }
        assertEquals(listOf(item), dao.linhas.value)
    }

    @Test
    fun editarSoONomeNaoConfereNemMexeNoFeito() = runTest {
        val item = oleoFeito()
        // Km atual abaixo do feito (um registro retroativo): o feito não mudou, então não é conferido.
        plano.salvar(item.copy(nome = "Troca de óleo"), kmAtual = 4_000)

        val salvo = dao.linhas.value.single()
        assertEquals("Troca de óleo", salvo.nome)
        assertEquals(item.feitoEm, salvo.feitoEm)
        assertEquals(item.ultimaVezKm, salvo.ultimaVezKm)
    }
}
