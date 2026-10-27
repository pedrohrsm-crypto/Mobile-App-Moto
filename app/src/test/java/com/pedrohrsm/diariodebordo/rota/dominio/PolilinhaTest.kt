package com.pedrohrsm.diariodebordo.rota.dominio

import com.pedrohrsm.diariodebordo.localizacao.Ponto
import org.junit.Assert.assertEquals
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
}
