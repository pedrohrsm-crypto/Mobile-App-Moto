package com.pedrohrsm.diariodebordo.plano.dominio

import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.OrigemItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrocaDeOleoTest {

    private fun item(nome: String, origem: OrigemItem = OrigemItem.USUARIO, excluido: Long? = null) = ItemDoPlano(
        id = nome,
        motoId = "moto",
        nome = nome,
        intervaloKm = 5_000,
        origem = origem,
        excluidoEm = excluido,
    )

    @Test
    fun achaOItemDoPlanoBase() {
        val itens = planoBase(null).map { item(it.nome, OrigemItem.PLANO_BASE) }
        assertEquals(NOME_TROCA_DE_OLEO, itemDeTrocaDeOleo(itens)?.nome)
    }

    @Test
    fun nomeExatoGanhaDoNomeComComplemento() {
        val itens = listOf(item("Troca de óleo 10W40"), item("TROCA DE OLEO"), item("Filtro de óleo"))
        assertEquals("TROCA DE OLEO", itemDeTrocaDeOleo(itens)?.nome)
        assertEquals("Troca de óleo 10W40", itemDeTrocaDeOleo(itens.drop(1).drop(1) + itens.first())?.nome)
    }

    @Test
    fun ignoraExcluidoPendenciaEOutrosOleos() {
        val itens = listOf(
            item(NOME_TROCA_DE_OLEO, excluido = 1),
            item(NOME_TROCA_DE_OLEO, OrigemItem.OCORRENCIA),
            item("Filtro de óleo"),
            item("Óleo da transmissão"),
        )
        assertNull(itemDeTrocaDeOleo(itens))
    }
}
