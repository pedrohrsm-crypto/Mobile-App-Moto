package com.pedrohrsm.diariodebordo.rota.dominio

import com.pedrohrsm.diariodebordo.localizacao.Ponto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PolilinhaTest {

    @Test
    fun exemploDaDocumentacaoDoAlgoritmo() {
        val pontos = listOf(Ponto(38.5, -120.2, 0), Ponto(40.7, -120.95, 0), Ponto(43.252, -126.453, 0))
        assertEquals("_p~iF~ps|U_ulLnnqC_mqNvxq`@", Polilinha.codificar(pontos))
    }

    @Test
    fun idaEVoltaGuardaSoLatitudeELongitude() {
        val pontos =
            listOf(Ponto(-23.55052, -46.63331, 1_000, 4f), Ponto(-23.56, -46.64, 2_000), Ponto(-22.9, -43.2, 3_000))
        val volta = Polilinha.decodificar(Polilinha.codificar(pontos))
        assertEquals(pontos.size, volta.size)
        pontos.zip(volta).forEach { (a, b) ->
            assertEquals(a.latitude, b.latitude, 1e-5)
            assertEquals(a.longitude, b.longitude, 1e-5)
            assertEquals(0L, b.instante)
            assertEquals(null, b.precisaoM)
        }
    }

    @Test
    fun vazioViraTextoVazio() {
        assertEquals("", Polilinha.codificar(emptyList()))
        assertEquals(emptyList<Ponto>(), Polilinha.decodificar(""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun textoCortadoEhRecusado() {
        Polilinha.decodificar("_p~iF~ps|U_ulL")
    }

    /** #78: traçado com partes ocultas tem mais de um trecho; ida e volta mantém os trechos separados. */
    @Test
    fun trechosIdaEVoltaMantemOsTrechosSeparados() {
        val trechos = listOf(
            listOf(Ponto(-27.5, -48.5, 0), Ponto(-27.51, -48.49, 0)),
            listOf(Ponto(-27.6, -48.4, 0), Ponto(-27.61, -48.39, 0), Ponto(-27.62, -48.38, 0)),
        )
        val texto = Polilinha.codificarTrechos(trechos)
        assertEquals(1, texto.count { it == Polilinha.SEPARADOR_DE_TRECHOS })
        val volta = Polilinha.decodificarTrechos(texto)
        assertEquals(listOf(2, 3), volta.map { it.size })
        assertEquals(-27.62, volta.last().last().latitude, 1e-5)
        assertEquals(emptyList<List<Ponto>>(), Polilinha.decodificarTrechos(""))
    }

    /**
     * Ler um traçado com trechos como se fosse um só desenharia uma reta por dentro do que ficou oculto. A recusa é
     * pelo separador, não por acaso: o texto com trechos às vezes também parece incompleto.
     */
    @Test
    fun decodificarUmTrechoSoRecusaTextoComTrechos() {
        val texto = Polilinha.codificarTrechos(listOf(listOf(Ponto(1.0, 1.0, 0)), listOf(Ponto(2.0, 2.0, 0))))
        val erro = assertThrows(IllegalArgumentException::class.java) { Polilinha.decodificar(texto) }
        assertEquals("Polilinha com mais de um trecho", erro.message)
    }
}
