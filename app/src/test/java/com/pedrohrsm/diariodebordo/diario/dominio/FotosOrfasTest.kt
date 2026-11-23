package com.pedrohrsm.diariodebordo.diario.dominio

import org.junit.Assert.assertEquals
import org.junit.Test

/** A regra única de órfão (#94): o que nenhuma foto ativa usa e passou da carência sai; na dúvida, fica. */
class FotosOrfasTest {

    private val agora = 1_000_000_000_000L
    private val velho = agora - CARENCIA_DAS_ORFAS_MS - 1

    @Test
    fun arquivoSemFotoAtivaForaDaCarenciaEhOrfao() {
        assertEquals(listOf("solta.jpg"), fotosOrfas(listOf(ArquivoNaPasta("solta.jpg", velho)), emptySet(), agora))
    }

    @Test
    fun arquivoDeFotoAtivaNuncaEhOrfaoPorMaisVelhoQueSeja() {
        val naPasta = listOf(ArquivoNaPasta("ativa.jpg", 1), ArquivoNaPasta("solta.jpg", velho))
        assertEquals(listOf("solta.jpg"), fotosOrfas(naPasta, setOf("ativa.jpg"), agora))
    }

    @Test
    fun carenciaDeDoisDiasContaAPartirDaModificacao() {
        assertEquals(2L * 24 * 60 * 60 * 1000, CARENCIA_DAS_ORFAS_MS)
        val naPasta = listOf(
            ArquivoNaPasta("no-limite.jpg", agora - CARENCIA_DAS_ORFAS_MS),
            ArquivoNaPasta("um-ms-antes.jpg", agora - CARENCIA_DAS_ORFAS_MS + 1),
        )
        assertEquals(listOf("no-limite.jpg"), fotosOrfas(naPasta, emptySet(), agora))
    }

    @Test
    fun dataDesconhecidaOuNoFuturoContaComoRecente() {
        val naPasta = listOf(
            ArquivoNaPasta("sem-data.jpg", 0),
            ArquivoNaPasta("relogio-voltou.jpg", agora + CARENCIA_DAS_ORFAS_MS * 10),
        )
        assertEquals(emptyList<String>(), fotosOrfas(naPasta, emptySet(), agora))
    }
}
