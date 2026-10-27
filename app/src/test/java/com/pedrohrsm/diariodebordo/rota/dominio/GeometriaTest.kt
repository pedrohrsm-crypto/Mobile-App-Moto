package com.pedrohrsm.diariodebordo.rota.dominio

import com.pedrohrsm.diariodebordo.localizacao.Ponto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometriaTest {

    private fun p(lat: Double, lon: Double, precisao: Float? = null) =
        Ponto(lat, lon, instante = 0, precisaoM = precisao)

    @Test
    fun umGrauDeLatitudeTemCercaDe111Km() {
        assertEquals(111_195.0, distanciaM(p(0.0, 0.0), p(1.0, 0.0)), 1.0)
    }

    @Test
    fun distanciaTotalSomaOsTrechos() {
        val rota = listOf(p(0.0, 0.0), p(0.001, 0.0), p(0.002, 0.0))
        assertEquals(2 * distanciaM(rota[0], rota[1]), distanciaTotalM(rota), 0.01)
        assertEquals(0.0, distanciaTotalM(listOf(p(0.0, 0.0))), 0.0)
    }

    @Test
    fun leituraImprecisaFicaForaEPrecisaoDesconhecidaFica() {
        val pontos = listOf(p(0.0, 0.0, 5f), p(0.1, 0.1, 80f), p(0.2, 0.2, null), p(0.3, 0.3, PRECISAO_MAXIMA_M))
        assertEquals(listOf(pontos[0], pontos[2], pontos[3]), semRuido(pontos))
    }

    @Test
    fun simplificarTiraPontosEmLinhaRetaEMantemACurva() {
        // Reta de ~1,1 km para o norte com um desvio de ~2 m no meio, depois uma curva de ~1,1 km para o leste.
        val reta = (0..10).map { p(it * 0.001, if (it == 5) 0.00002 else 0.0) }
        val curva = (1..10).map { p(0.01, it * 0.001) }
        val simples = simplificar(reta + curva)
        assertEquals(listOf(p(0.0, 0.0), p(0.01, 0.0), p(0.01, 0.01)), simples)
    }

    @Test
    fun simplificarNaoMexeEmRotaCurta() {
        val dois = listOf(p(0.0, 0.0), p(1.0, 1.0))
        assertEquals(dois, simplificar(dois))
    }

    @Test
    fun projetarCabeNaAreaComNorteParaCima() {
        val tela = projetar(listOf(p(0.0, 0.0), p(0.01, 0.0)), largura = 200f, altura = 100f, margem = 10f)
        val (sul, norte) = tela
        assertTrue(norte.second < sul.second)
        tela.forEach { (x, y) -> assertTrue(x in 10f..190f && y in 10f..90f) }
        assertEquals(10f, norte.second, 0.01f)
        assertEquals(90f, sul.second, 0.01f)
    }
}
