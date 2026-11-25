package com.pedrohrsm.diariodebordo.backup.dominio

import com.pedrohrsm.diariodebordo.localizacao.Ponto
import com.pedrohrsm.diariodebordo.rota.data.Rota
import com.pedrohrsm.diariodebordo.rota.dominio.CorteDasPontas
import com.pedrohrsm.diariodebordo.rota.dominio.Polilinha
import com.pedrohrsm.diariodebordo.rota.dominio.ProtecaoDasRotas
import com.pedrohrsm.diariodebordo.rota.dominio.Trecho
import com.pedrohrsm.diariodebordo.rota.dominio.ZonaDePrivacidade
import com.pedrohrsm.diariodebordo.rota.dominio.distanciaM
import com.pedrohrsm.diariodebordo.rota.dominio.dividirTracado
import com.pedrohrsm.diariodebordo.rota.dominio.paraOTracadoJaCortado
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
        val chegando =
            listOf(vinda).paraOAparelho(tracadosProtegidos = true, pontasCortadasM = 0, doAparelho = listOf(rota))
        assertEquals(listOf(vinda.copy(polilinha = tracado)), chegando)
    }

    @Test
    fun rotaQueOAparelhoNaoTemChegaComoVeio() {
        val vinda = rota.copy(id = "nova", polilinha = "cortado")
        assertEquals(
            listOf(vinda),
            listOf(vinda).paraOAparelho(tracadosProtegidos = true, pontasCortadasM = 0, doAparelho = listOf(rota)),
        )
    }

    @Test
    fun arquivoSemProtecaoChegaIgual() {
        val vinda = rota.copy(polilinha = "outro", alteradoEm = 20)
        assertEquals(
            listOf(vinda),
            listOf(vinda).paraOAparelho(tracadosProtegidos = false, pontasCortadasM = 0, doAparelho = listOf(rota)),
        )
    }

    // #88: a rota que chegou já sem as pontas.

    private val km1 = ProtecaoDasRotas(CorteDasPontas.KM1)
    private val segredoDeOutroAparelho = ByteArray(32) { (it * 7 + 3).toByte() }
    private val jaCortada = rota.copy(
        id = "restaurada",
        polilinha = rota.paraOArquivo(km1, segredoDeOutroAparelho).polilinha,
        pontasJaCortadasM = CorteDasPontas.KM1.metros,
    )

    /** (a) */
    @Test
    fun rotaJaCortadaExportadaDeNovoSaiComAMesmaPolilinha() {
        assertNotEquals("o outro aparelho cortou", tracado, jaCortada.polilinha)
        assertEquals(jaCortada, jaCortada.paraOArquivo(km1, segredo))
    }

    /** (b) */
    @Test
    fun rotaJaCortadaComZonaNovaPerdeOTrechoDaZona() {
        val meio = Polilinha.decodificarTrechos(jaCortada.polilinha).flatten().let { it[it.size / 2] }
        val zona = ZonaDePrivacidade(nome = "Nova", latitude = meio.latitude, longitude = meio.longitude, raioM = 500)

        val saida = jaCortada.paraOArquivo(ProtecaoDasRotas(CorteDasPontas.KM1, listOf(zona)), segredo)

        val antes = Polilinha.decodificarTrechos(jaCortada.polilinha)
        val depois = Polilinha.decodificarTrechos(saida.polilinha)
        assertEquals("a zona parte a rota em dois", 2, depois.size)
        val pontos = depois.flatten()
        assertTrue(pontos.none { distanciaM(it, meio) < zona.raioM })
        assertEquals("as pontas não são cortadas de novo", antes.first().first(), pontos.first())
        assertEquals("as pontas não são cortadas de novo", antes.last().last(), pontos.last())
    }

    /** (c) */
    @Test
    fun rotaGravadaNoAparelhoContinuaPerdendoAsPontas() {
        assertEquals(0, rota.pontasJaCortadasM)
        val saida = Polilinha.decodificarTrechos(rota.paraOArquivo(km1, segredo).polilinha).flatten()
        val original = Polilinha.decodificarTrechos(tracado).flatten()
        assertTrue(saida.all { distanciaM(it, original.first()) >= 800 && distanciaM(it, original.last()) >= 800 })
    }

    /** O corte subiu depois da restauração: os 5 km de agora valem, e a rota perde as pontas outra vez. */
    @Test
    fun corteMaiorQueOJaSofridoCortaAsPontasDeNovo() {
        val km2 = ProtecaoDasRotas(CorteDasPontas.KM2)
        val saida = Polilinha.decodificarTrechos(jaCortada.paraOArquivo(km2, segredo).polilinha).flatten()
        val antes = Polilinha.decodificarTrechos(jaCortada.polilinha).flatten()
        assertTrue(saida.all { distanciaM(it, antes.first()) >= 1_600 && distanciaM(it, antes.last()) >= 1_600 })
    }

    @Test
    fun corteDesligadoNaRotaJaCortadaNaoMudaNada() {
        val semZona = ProtecaoDasRotas(CorteDasPontas.DESLIGADO)
        assertSame(jaCortada, jaCortada.paraOArquivo(semZona, segredo))
        assertEquals(km1.copy(corte = CorteDasPontas.DESLIGADO), km1.paraOTracadoJaCortado(1_000))
        assertEquals(km1, km1.paraOTracadoJaCortado(999))
        assertEquals(km1, km1.paraOTracadoJaCortado(0))
    }

    /** (d) */
    @Test
    fun rotaQueOAparelhoJaTinhaMantemAPolilinhaCompletaENaoFicaMarcada() {
        val vinda = rota.copy(polilinha = "cortado", alteradoEm = 20)
        val chegando = listOf(vinda).paraOAparelho(
            tracadosProtegidos = true,
            pontasCortadasM = CorteDasPontas.KM1.metros,
            doAparelho = listOf(rota),
        )
        assertEquals(listOf(vinda.copy(polilinha = tracado, pontasJaCortadasM = 0)), chegando)
    }

    @Test
    fun rotaQueOAparelhoNaoTinhaChegaMarcadaComOCorteDoArquivo() {
        val vinda = rota.copy(id = "nova", polilinha = "cortado")
        val chegando = listOf(vinda).paraOAparelho(
            tracadosProtegidos = true,
            pontasCortadasM = CorteDasPontas.KM2.metros,
            doAparelho = listOf(rota),
        )
        assertEquals(listOf(vinda.copy(pontasJaCortadasM = 2_000)), chegando)
    }

    /** A marca do aparelho fica com a polilinha do aparelho: a rota já cortada continua marcada. */
    @Test
    fun rotaJaMarcadaNoAparelhoContinuaMarcada() {
        val vinda = jaCortada.copy(alteradoEm = 20, pontasJaCortadasM = 0)
        val chegando = listOf(vinda).paraOAparelho(
            tracadosProtegidos = true,
            pontasCortadasM = 0,
            doAparelho = listOf(jaCortada),
        )
        assertEquals(listOf(vinda.copy(pontasJaCortadasM = 1_000)), chegando)
    }

    /** O pior caso do #88: arquivo sem proteção traz a rota inteira, e ela nunca pode ficar marcada. */
    @Test
    fun arquivoSemProtecaoNuncaMarcaARota() {
        val vinda = rota.copy(id = "nova", pontasJaCortadasM = 5_000)
        val chegando = listOf(vinda).paraOAparelho(
            tracadosProtegidos = false,
            pontasCortadasM = 5_000,
            doAparelho = emptyList(),
        )
        assertEquals(listOf(vinda.copy(pontasJaCortadasM = 0)), chegando)
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
