package com.pedrohrsm.diariodebordo.backup.dominio

import com.pedrohrsm.diariodebordo.localizacao.Ponto
import com.pedrohrsm.diariodebordo.rota.data.Rota
import com.pedrohrsm.diariodebordo.rota.dominio.CorteDasPontas
import com.pedrohrsm.diariodebordo.rota.dominio.Polilinha
import com.pedrohrsm.diariodebordo.rota.dominio.ProtecaoDasRotas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
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
}
