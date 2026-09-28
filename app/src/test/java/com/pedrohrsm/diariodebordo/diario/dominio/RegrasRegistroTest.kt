package com.pedrohrsm.diariodebordo.diario.dominio

import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegrasRegistroTest {

    @Test
    fun camposOpcionaisAceitamVazio() {
        assertTrue(validarRegistro(TipoRegistro.NOTA, "Nota", "", "", "", 0).isEmpty())
    }

    @Test
    fun camposPreenchidosPrecisamSerValidos() {
        val invalidos = validarRegistro(TipoRegistro.ABASTECIMENTO, " ", "12,5", "abc", "0", MAXIMO_FOTOS + 1)
        assertEquals(
            setOf(
                CampoRegistro.TITULO,
                CampoRegistro.KM,
                CampoRegistro.VALOR,
                CampoRegistro.LITROS,
                CampoRegistro.FOTOS,
            ),
            invalidos,
        )
    }

    @Test
    fun fotoGrandeAbreReduzidaSemFicarMenorQueOLimite() {
        assertEquals(1, fatorDeReducao(1200, 800))
        assertEquals(2, fatorDeReducao(4000, 3000))
        assertEquals(4, fatorDeReducao(8000, 6000))
        assertEquals(0.4, escalaFinal(4000, 3000), 0.0001)
        assertEquals(1.0, escalaFinal(1000, 800), 0.0)
    }
}
