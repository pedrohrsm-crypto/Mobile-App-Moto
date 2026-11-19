package com.pedrohrsm.diariodebordo.backup.dominio

import com.pedrohrsm.diariodebordo.localizacao.Ponto
import com.pedrohrsm.diariodebordo.rota.data.Rota
import com.pedrohrsm.diariodebordo.rota.dominio.CorteDasPontas
import com.pedrohrsm.diariodebordo.rota.dominio.Polilinha
import com.pedrohrsm.diariodebordo.rota.dominio.ProtecaoDasRotas
import com.pedrohrsm.diariodebordo.rota.dominio.Trecho
import com.pedrohrsm.diariodebordo.rota.dominio.ZonaDePrivacidade
import com.pedrohrsm.diariodebordo.rota.dominio.dividirTracado
import com.pedrohrsm.diariodebordo.rota.dominio.tracadoDaRota
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** #78: o que muda numa rota que sai do aparelho, e o que fica ao voltar. */
class TracadosDasRotasTest {

    private val segredo = ByteArray(32) { it.toByte() }
    private val tracado = Polilinha.codificar((0..100).map { Ponto(-27.5, -48.5 + it * 0.001, 0) })
    private val rota = Rota(
        id = "r",
        motoId = "m",
        iniciadaEm = 1,
        distanciaM = 9_876,
        duracaoS = 1_234,
        polilinha = tracado,
        emGravacao = false,
        alteradoEm = 10,
    )

    @Test
    fun paraOArquivoCortaOTracadoEMantemDistanciaEDuracaoReais() {
        val saida = rota.paraOArquivo(ProtecaoDasRotas(CorteDasPontas.KM1), segredo)
        assertNotEquals(tracado, saida.polilinha)
        assertEquals(rota.copy(polilinha = saida.polilinha), saida)
    }

    @Test
    fun paraOArquivoSemProtecaoDevolveARotaComoEsta() {
        assertSame(rota, rota.paraOArquivo(ProtecaoDasRotas(CorteDasPontas.DESLIGADO), segredo))
    }

    @Test
    fun arquivoProtegidoNaoApagaOTracadoCompletoDoAparelho() {
        val vinda = rota.copy(polilinha = "cortado", alteradoEm = 20, distanciaM = 1)
        val chegando = listOf(vinda).paraOAparelho(tracadosProtegidos = true, doAparelho = listOf(rota))
        assertEquals(listOf(vinda.copy(polilinha = tracado)), chegando)
    }

    @Test
    fun rotaQueOAparelhoNaoTemChegaComoVeio() {
        val vinda = rota.copy(id = "nova", polilinha = "cortado")
        assertEquals(listOf(vinda), listOf(vinda).paraOAparelho(tracadosProtegidos = true, doAparelho = listOf(rota)))
    }

    @Test
    fun arquivoSemProtecaoChegaIgual() {
        val vinda = rota.copy(polilinha = "outro", alteradoEm = 20)
        assertEquals(listOf(vinda), listOf(vinda).paraOAparelho(tracadosProtegidos = false, doAparelho = listOf(rota)))
    }

    /** Uma zona engole a rota inteira, sem corte nas pontas: nada do traçado sai, só distância e duração. */
    @Test
    fun zonaQueCobreOTrajetoTodoSaiSemTracado() {
        val curta = rota.copy(
            polilinha = Polilinha.codificar((0..3).map { Ponto(-27.5, -48.5 + it * 0.001, 0) }),
        )
        val zona = ZonaDePrivacidade(nome = "Casa", latitude = -27.5, longitude = -48.4985, raioM = 500)
        val protecao = ProtecaoDasRotas(CorteDasPontas.DESLIGADO, listOf(zona))

        val tracado = protecao.tracadoDaRota(curta.id, curta.polilinha, segredo)
        assertTrue("tudo oculto", tracado.isNotEmpty() && tracado.all { it.oculto })
        val saida = curta.paraOArquivo(protecao, segredo)
        assertEquals("", saida.polilinha)
        assertEquals(curta.copy(polilinha = ""), saida)
    }

    /** Rota sem traçado com a proteção ativa: nada a cortar, e nada quebra no caminho. */
    @Test
    fun trajetoVazioComProtecaoAtivaSaiVazio() {
        val zona = ZonaDePrivacidade(nome = "Casa", latitude = -27.5, longitude = -48.5, raioM = 500)
        val protecao = ProtecaoDasRotas(CorteDasPontas.KM1, listOf(zona))
        val vazia = rota.copy(polilinha = "")

        assertEquals(vazia, vazia.paraOArquivo(protecao, segredo))
        assertEquals(emptyList<Trecho>(), protecao.tracadoDaRota(vazia.id, "", segredo))
        assertEquals(emptyList<Trecho>(), dividirTracado(emptyList(), 1_000.0, listOf(zona)))
    }
}
