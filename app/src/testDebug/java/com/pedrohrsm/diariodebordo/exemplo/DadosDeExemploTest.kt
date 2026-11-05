package com.pedrohrsm.diariodebordo.exemplo

import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import com.pedrohrsm.diariodebordo.plano.dominio.situacao
import com.pedrohrsm.diariodebordo.rota.dominio.Polilinha
import com.pedrohrsm.diariodebordo.rota.dominio.distanciaTotalM
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gerador dos dados de exemplo (#77), na JVM: quantidades, km coerente, plano variado e traçado dentro da estrada. */
class DadosDeExemploTest {

    private val fuso = ZoneId.of("America/Sao_Paulo")
    private val agora = Instant.parse("2026-11-10T12:00:00Z")
    private val dados = gerarDadosDeExemplo(agora, fuso)

    @Test
    fun geraAsQuantidadesPrometidas() {
        assertEquals(MOTOS_DE_EXEMPLO, dados.motos.size)
        assertEquals(ROTAS_DE_EXEMPLO, dados.rotas.size)
        assertEquals(2 * (MESES_DE_EXEMPLO + 1), dados.leituras.size)
        val porTipo = dados.registros.groupingBy { it.tipo }.eachCount()
        assertTrue("faltou abastecimento", (porTipo[TipoRegistro.ABASTECIMENTO] ?: 0) >= 2)
        assertTrue("faltou manutenção", (porTipo[TipoRegistro.MANUTENCAO] ?: 0) >= 2)
        assertTrue("faltou nota", (porTipo[TipoRegistro.NOTA] ?: 0) >= 2)
        // Todo rolê aponta para uma das rotas geradas, e toda rota tem o seu rolê.
        val idsDasRotas = dados.rotas.map { it.id }.toSet()
        val rotasDosRoles = dados.registros.filter { it.tipo == TipoRegistro.ROLE }.mapNotNull { it.rotaId }.toSet()
        assertEquals(idsDasRotas, rotasDosRoles)
    }

    @Test
    fun kmDeCadaMotoSoSobeEOsRegistrosBatemComALeitura() {
        dados.motos.forEach { moto ->
            val leituras = dados.leituras.filter { it.motoId == moto.id }.sortedBy { it.lidaEm }
            assertTrue(
                "km repetido ou voltando em ${moto.apelido}",
                leituras.zipWithNext().all { (a, b) -> b.km > a.km },
            )
            val porData = leituras.associate { it.lidaEm to it.km }
            dados.registros.filter { it.motoId == moto.id && it.hodometroKm != null }.forEach { r ->
                assertEquals("km de ${r.titulo} difere da leitura da data", porData[r.dataEm], r.hodometroKm)
            }
        }
    }

    @Test
    fun oPlanoTemItensEmDiaProximosEVencidos() {
        val relogio = Clock.fixed(agora, fuso)
        val kmAtual = dados.leituras.groupBy { it.motoId }.mapValues { (_, ls) -> ls.maxOf { it.km } }
        val urgencias = dados.itensDoPlano.map { it.situacao(kmAtual[it.motoId], relogio)!!.urgencia }
        assertEquals(setOf(Urgencia.EM_DIA, Urgencia.PROXIMO, Urgencia.VENCIDO), urgencias.toSet())
        // Cada moto mostra ao menos um item que pede atenção.
        dados.motos.forEach { moto ->
            val dela = dados.itensDoPlano.filter { it.motoId == moto.id }
                .map { it.situacao(kmAtual[moto.id], relogio)!!.urgencia }
            assertTrue("sem item que pede atenção em ${moto.apelido}", dela.any { it != Urgencia.EM_DIA })
        }
    }

    @Test
    fun nenhumPontoDaRotaSaiDaCaixaDaEstrada() {
        dados.rotas.forEach { rota ->
            val pontos = Polilinha.decodificar(rota.polilinha)
            assertTrue("rota sem traçado", pontos.size > 10)
            val fora = pontos.filterNot { CAIXA_RIO_DO_RASTRO.contem(it) }
            assertTrue("pontos fora da caixa da estrada: $fora", fora.isEmpty())
        }
    }

    @Test
    fun aDistanciaEADuracaoDaRotaBatemComOTracado() {
        dados.rotas.forEach { rota ->
            val pontos = Polilinha.decodificar(rota.polilinha)
            // A polilinha guarda 1e-5 grau (cerca de 1 m) por ponto: a soma pode diferir um pouco da conta original.
            assertEquals(rota.distanciaM.toDouble(), distanciaTotalM(pontos), rota.distanciaM * 0.02)
            assertTrue(rota.distanciaM > 1_000)
            assertEquals((rota.terminadaEm!! - rota.iniciadaEm) / 1_000, rota.duracaoS)
            assertTrue(!rota.emGravacao)
        }
    }

    @Test
    fun osIdsSaoFixosEUnicosParaCarregarDuasVezesNaoDuplicar() {
        val outra = gerarDadosDeExemplo(agora.plusSeconds(3_600), fuso)
        fun ids(d: DadosDeExemplo) = d.motos.map { it.id } + d.leituras.map { it.id } + d.registros.map { it.id } +
            d.itensDoPlano.map { it.id } + d.rotas.map { it.id }
        assertEquals(ids(dados), ids(outra))
        assertEquals(ids(dados).size, ids(dados).toSet().size)
    }

    @Test
    fun trajetoSinteticoRepeteIgualEExigePontosSuficientes() {
        val a = trajetoSintetico(-28.41 to -49.56, -28.39 to -49.54, 20, 0.5, 0L, 1_000)
        assertEquals(a, trajetoSintetico(-28.41 to -49.56, -28.39 to -49.54, 20, 0.5, 0L, 1_000))
        val erro = runCatching { trajetoSintetico(-28.41 to -49.56, -28.39 to -49.54, 1, 0.0, 0L, 1_000) }
        assertTrue(erro.exceptionOrNull() is IllegalArgumentException)
    }
}
