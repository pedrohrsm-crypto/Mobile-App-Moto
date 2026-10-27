package com.pedrohrsm.diariodebordo.plano.dominio

import com.pedrohrsm.diariodebordo.garagem.data.CategoriaMoto
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.OrigemItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrocaDeOleoTest {

    private fun item(
        nome: String,
        chave: String? = null,
        origem: OrigemItem = OrigemItem.USUARIO,
        excluido: Long? = null,
    ) = ItemDoPlano(
        id = nome,
        motoId = "moto",
        chave = chave,
        nome = nome,
        intervaloKm = 5_000,
        origem = origem,
        excluidoEm = excluido,
    )

    @Test
    fun todoPlanoBaseTemUmItemDeTrocaDeOleoComAChaveEstavel() {
        (CategoriaMoto.entries + listOf(null)).forEach { categoria ->
            val itens = planoBase(categoria).map { item(it.nome, it.chave, OrigemItem.PLANO_BASE) }
            assertEquals(NOME_TROCA_DE_OLEO, itemDeTrocaDeOleo(itens)?.nome)
            assertEquals(1, planoBase(categoria).count { it.chave == CHAVE_TROCA_DE_OLEO })
        }
    }

    @Test
    fun renomearOItemNaoPerdeOAtalho() {
        val itens = listOf(item("Óleo 10W40 do mês", CHAVE_TROCA_DE_OLEO), item("Filtro de óleo"))
        assertEquals("Óleo 10W40 do mês", itemDeTrocaDeOleo(itens)?.nome)
    }

    @Test
    fun itemSemAChaveNaoEhOAtalhoMesmoComONomeIgual() {
        val itens = listOf(item(NOME_TROCA_DE_OLEO), item("TROCA DE OLEO"), item("Óleo da transmissão"))
        assertNull(itemDeTrocaDeOleo(itens))
    }

    @Test
    fun ignoraExcluidoEPendenciaMesmoComAChave() {
        val itens = listOf(
            item("a", CHAVE_TROCA_DE_OLEO, excluido = 1),
            item("b", CHAVE_TROCA_DE_OLEO, OrigemItem.OCORRENCIA),
        )
        assertNull(itemDeTrocaDeOleo(itens))
    }
}
