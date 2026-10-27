package com.pedrohrsm.diariodebordo.backup.dominio

import org.junit.Assert.assertEquals
import org.junit.Test

class MesclagemTest {

    private data class Linha(val id: String, val alteradoEm: Long, val texto: String = "")

    private fun mesclar(locais: List<Linha>, chegando: List<Linha>) = mesclar(locais, chegando, {
        it.id
    }, { it.alteradoEm })

    @Test
    fun novoEntraMaisRecenteSubstituiMaisAntigoFica() {
        val locais = listOf(Linha("a", 10, "local"), Linha("b", 10, "local"), Linha("c", 10, "local"))
        val chegando = listOf(Linha("a", 20, "arquivo"), Linha("b", 5, "arquivo"), Linha("d", 1, "arquivo"))
        val m = mesclar(locais, chegando)
        assertEquals(listOf(Linha("a", 20, "arquivo"), Linha("d", 1, "arquivo")), m.gravar)
        assertEquals(1, m.novos)
        assertEquals(1, m.atualizados)
    }

    @Test
    fun empateFicaComOAparelho() {
        val m = mesclar(listOf(Linha("a", 10, "local")), listOf(Linha("a", 10, "arquivo")))
        assertEquals(emptyList<Linha>(), m.gravar)
    }

    @Test
    fun idRepetidoNoArquivoValeOMaisRecente() {
        val m = mesclar(emptyList(), listOf(Linha("a", 1, "velho"), Linha("a", 9, "novo"), Linha("a", 3, "meio")))
        assertEquals(listOf(Linha("a", 9, "novo")), m.gravar)
        assertEquals(1, m.novos)
    }

    @Test
    fun itemProtegidoDoAparelhoNaoEhSubstituido() {
        val excluida = Linha("rota", 10, "")
        val m =
            mesclar(listOf(excluida), listOf(Linha("rota", 99, "traçado")), {
                it.id
            }, { it.alteradoEm }, { it.texto.isEmpty() })
        assertEquals(emptyList<Linha>(), m.gravar)
        assertEquals(0, m.atualizados)
    }
}
