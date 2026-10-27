package com.pedrohrsm.diariodebordo.rota.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissoesGravacaoTest {

    private val fina = "android.permission.ACCESS_FINE_LOCATION"
    private val aproximada = "android.permission.ACCESS_COARSE_LOCATION"
    private val notificacao = "android.permission.POST_NOTIFICATIONS"
    private val android12 = 32
    private val android13 = 33

    private fun so(vararg concedidas: String): (String) -> Boolean = { it in concedidas }

    /** O defeito da revisão: com a localização já concedida, o Android 13+ gravava sem a notificação fixa. */
    @Test
    fun localizacaoConcedidaNoAndroid13AindaPedeANotificacao() {
        assertEquals(listOf(notificacao), PermissoesGravacao.queFaltam(so(fina), android13))
        assertEquals(DecisaoGravacao.SEM_NOTIFICACAO, PermissoesGravacao.decidir(so(fina), android13))
    }

    @Test
    fun antesDoAndroid13ALocalizacaoBasta() {
        assertEquals(emptyList<String>(), PermissoesGravacao.queFaltam(so(aproximada), android12))
        assertEquals(DecisaoGravacao.GRAVAR, PermissoesGravacao.decidir(so(aproximada), android12))
    }

    @Test
    fun semNadaPedeAsDuasLocalizacoesEANotificacao() {
        assertEquals(listOf(fina, aproximada, notificacao), PermissoesGravacao.queFaltam(so(), android13))
        assertEquals(DecisaoGravacao.SEM_LOCALIZACAO, PermissoesGravacao.decidir(so(notificacao), android13))
    }

    @Test
    fun comTudoConcedidoGravaSemPerguntar() {
        assertEquals(emptyList<String>(), PermissoesGravacao.queFaltam(so(fina, notificacao), android13))
        assertEquals(DecisaoGravacao.GRAVAR, PermissoesGravacao.decidir(so(fina, notificacao), android13))
    }
}
