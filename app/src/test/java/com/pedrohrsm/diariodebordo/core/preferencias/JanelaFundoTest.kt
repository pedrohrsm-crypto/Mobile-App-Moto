package com.pedrohrsm.diariodebordo.core.preferencias

import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.fundoDaJanela
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Abertura a frio sem piscar: antes do app carregar, a janela usa `janela_fundo` (themes.xml), que precisa ser o
 * fundo do tema de partida; depois, a MainActivity pinta a janela com o fundo do tema salvo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JanelaFundoTest {

    private fun hex(cor: Int) = "#%08x".format(cor)

    @Test
    fun janelaFundoEhOFundoDoTemaDePartida() {
        val janela = RuntimeEnvironment.getApplication().getColor(R.color.janela_fundo)
        assertEquals(hex(fundoDaJanela(AjusteTema())), hex(janela))
    }

    @Test
    fun fundoDaJanelaSegueOTemaSalvo() {
        val fundos = TemaDiario.entries.map { hex(fundoDaJanela(AjusteTema(it))) }
        assertEquals("cada tema pinta a janela com o próprio fundo", 3, fundos.toSet().size)
        assertEquals(hex(fundoDaJanela(AjusteTema(TemaDiario.ESTRADA))), fundos[TemaDiario.ESTRADA.ordinal])
    }
}
