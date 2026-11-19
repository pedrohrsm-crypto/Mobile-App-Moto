package com.pedrohrsm.diariodebordo.rota.dominio

import com.pedrohrsm.diariodebordo.localizacao.Ponto
import kotlin.math.PI
import kotlin.math.cos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #78: o que sai oculto no arquivo exportado. As distâncias conferidas aqui são as da esfera (haversine,
 * [distanciaM]), não as da projeção plana usada no corte: o teste não confia na conta que está testando.
 * Traçados inventados, numa reta para o leste a partir de um ponto qualquer, longe de endereço real.
 */
class PrivacidadeTest {

    private val segredo = ByteArray(32) { (it * 7).toByte() }

    /** Ponto a [metros] para o leste da origem. */
    private fun leste(metros: Double) = Ponto(LAT, LON + metros / (METROS_POR_GRAU * cos(LAT * PI / 180)), 0)

    /** Reta de [km] com um ponto a cada 100 m. */
    private fun reta(km: Int) = (0..km * 10).map { leste(it * 100.0) }

    /** Pontos ao longo de cada segmento visível, para ver se alguma reta do arquivo passa por dentro de um círculo. */
    private fun amostras(trechos: List<List<Ponto>>): List<Ponto> = trechos.flatMap { trecho ->
        trecho.zipWithNext { a, b ->
            (0..20).map { i ->
                val t = i / 20.0
                Ponto(a.latitude + (b.latitude - a.latitude) * t, a.longitude + (b.longitude - a.longitude) * t, 0)
            }
        }.flatten()
    }

    @Test
    fun nenhumPontoDoArquivoFicaDentroDasPontasCortadas() {
        val pontos = reta(10)
        val protecao = ProtecaoDasRotas(CorteDasPontas.KM1)
        val corte = protecao.corteDaRotaM(segredo, "rota-1")
        val visiveis = protecao.tracadoDaRota("rota-1", Polilinha.codificar(pontos), segredo).visiveis()
        assertTrue("sobra traçado no meio", visiveis.isNotEmpty())
        amostras(visiveis).forEach {
            assertTrue("perto do começo", distanciaM(it, pontos.first()) >= corte)
            assertTrue("perto do fim", distanciaM(it, pontos.last()) >= corte)
        }
    }

    /** Estrada reta: a simplificação deixa só os dois extremos, e o corte acontece no meio do segmento. */
    @Test
    fun retaSemPontosNoMeioSaiCortadaNaBorda() {
        val pontos = listOf(leste(0.0), leste(20_000.0))
        val visiveis = dividirTracado(listOf(pontos), 1_000.0, emptyList()).visiveis()
        assertEquals(1, visiveis.size)
        val (ini, fim) = visiveis.single().first() to visiveis.single().last()
        assertEquals(1_010.0, distanciaM(ini, pontos.first()), 5.0)
        assertEquals(1_010.0, distanciaM(fim, pontos.last()), 5.0)
    }

    @Test
    fun nenhumPontoDoArquivoFicaDentroDeUmaZonaNoMeioDaRota() {
        val pontos = reta(10)
        val meio = leste(5_000.0)
        val zona =
            ZonaDePrivacidade(nome = "Trabalho", latitude = meio.latitude, longitude = meio.longitude, raioM = 500)
        val visiveis = dividirTracado(listOf(pontos), 0.0, listOf(zona)).visiveis()
        assertEquals("a zona parte a rota em dois", 2, visiveis.size)
        amostras(visiveis).forEach { assertTrue("dentro da zona", distanciaM(it, meio) >= 500) }
    }

    /** Duas leituras longe uma da outra, com a zona entre elas: a reta do arquivo não passa por dentro dela. */
    @Test
    fun segmentoQueAtravessaUmaZonaECortadoNasDuasBordas() {
        val centro = leste(3_000.0)
        val zona =
            ZonaDePrivacidade(nome = "Casa", latitude = centro.latitude, longitude = centro.longitude, raioM = 200)
        val trechos = dividirTracado(listOf(listOf(leste(0.0), leste(6_000.0))), 0.0, listOf(zona))
        assertEquals(listOf(false, true, false), trechos.map { it.oculto })
        amostras(trechos.visiveis()).forEach { assertTrue(distanciaM(it, centro) >= 200) }
    }

    @Test
    fun rotaCurtaDemaisParaCortarAsDuasPontasSaiSemTracado() {
        val pontos = reta(1) + leste(1_500.0)
        val tracado = ProtecaoDasRotas(CorteDasPontas.KM1).tracadoDaRota("curta", Polilinha.codificar(pontos), segredo)
        assertTrue("tudo oculto", tracado.all { it.oculto })
        assertEquals(emptyList<List<Ponto>>(), tracado.visiveis())
    }

    @Test
    fun protecaoDesligadaESemZonasDevolveOTracadoInteiro() {
        val pontos = reta(3)
        val tracado = ProtecaoDasRotas(
            CorteDasPontas.DESLIGADO,
        ).tracadoDaRota("r", Polilinha.codificar(pontos), segredo)
        assertEquals(1, tracado.size)
        assertEquals(pontos.size, tracado.single().pontos.size)
        assertTrue(tracado.none { it.oculto })
    }

    /** O detalhe da rota desenha oculto, visível, oculto, e os pedaços se tocam na borda (sem buraco no desenho). */
    @Test
    fun pedacosOcultosEVisiveisSeAlternamESeTocam() {
        val trechos = dividirTracado(listOf(reta(10)), 1_000.0, emptyList())
        assertEquals(listOf(true, false, true), trechos.map { it.oculto })
        trechos.zipWithNext { a, b -> assertEquals(a.pontos.last(), b.pontos.first()) }
    }

    @Test
    fun fatorDoCorteEEstavelPorRota() {
        assertEquals(fatorDoCorte(segredo, "rota-1"), fatorDoCorte(segredo, "rota-1"), 0.0)
        val protecao = ProtecaoDasRotas(CorteDasPontas.KM2)
        assertEquals(protecao.corteDaRotaM(segredo, "rota-1"), protecao.corteDaRotaM(segredo.copyOf(), "rota-1"), 0.0)
    }

    /** ±20%: dentro da faixa, espalhado por ela, e diferente de um aparelho (segredo) para outro. */
    @Test
    fun fatorDoCorteVariaAte20PorCentoEntreRotas() {
        val fatores = (1..500).map { fatorDoCorte(segredo, "rota-$it") }
        assertTrue(fatores.all { it in 0.8..1.2 })
        assertTrue("usa a faixa de baixo", fatores.min() < 0.82)
        assertTrue("usa a faixa de cima", fatores.max() > 1.18)
        assertTrue("cada rota tem o seu", fatores.distinct().size > 490)
        assertNotEquals(fatorDoCorte(segredo, "rota-1"), fatorDoCorte(ByteArray(32), "rota-1"), 0.0)
    }

    /** A coordenada da zona não vai para mensagem de erro nem para relatório de falha. */
    @Test
    fun zonaNaoMostraOCentroNoTexto() {
        val zona =
            ZonaDePrivacidade(id = "z", nome = "Casa", latitude = -27.123456, longitude = -48.654321, raioM = 500)
        val texto = zona.toString()
        assertTrue(texto, "27.12" !in texto && "48.65" !in texto && "Casa" !in texto)
    }

    private companion object {
        const val LAT = -27.5
        const val LON = -48.5
        const val METROS_POR_GRAU = 111_194.9
    }
}
