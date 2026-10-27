package com.pedrohrsm.diariodebordo.plano.ui

import com.pedrohrsm.diariodebordo.navegacao.Abertura
import com.pedrohrsm.diariodebordo.navegacao.Destino
import com.pedrohrsm.diariodebordo.navegacao.Rotas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** As rotas do plano: a que a notificação abre e a que o formulário edita. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RotasDoPlanoTest {

    @Test
    fun planoDaMotoCarregaOItemDestacadoCodificado() {
        assertEquals("moto/m1/plano?destaque=i1", Rotas.planoDaMoto("m1", "i1"))
        assertEquals("moto/m1/plano?destaque=a%20b%26c", Rotas.planoDaMoto("m1", "a b&c"))
        assertEquals("moto/m1/plano", Rotas.planoDaMoto("m1", null))
    }

    @Test
    fun planoEhUmaAbaEAsRotasDeItemSaoDeTelaCheia() {
        assertTrue(Destino.entries.any { it.rota == Rotas.PLANO })
        assertTrue(Destino.entries.none { it.rota == Rotas.PLANO_DA_MOTO || it.rota == Rotas.ITEM })
        assertEquals("plano/item/abc", Rotas.item("abc"))
    }

    @Test
    fun aberturaGuardaMotoEItem() {
        assertEquals(Abertura("m1", "i1"), Abertura(motoId = "m1", itemId = "i1"))
    }
}
