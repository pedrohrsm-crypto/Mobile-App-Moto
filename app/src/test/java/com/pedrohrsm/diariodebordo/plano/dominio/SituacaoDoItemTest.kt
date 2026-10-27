package com.pedrohrsm.diariodebordo.plano.dominio

import com.pedrohrsm.diariodebordo.diario.data.TipoOcorrencia
import com.pedrohrsm.diariodebordo.garagem.data.CategoriaMoto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SituacaoDoItemTest {

    private val fuso = ZoneId.of("America/Sao_Paulo")
    private val feitoEm = Instant.parse("2026-01-10T15:00:00Z")
    private val agora = Instant.parse("2026-03-11T15:00:00Z") // 60 dias depois

    private fun situacao(intervalo: Intervalo, kmAtual: Int) =
        situacaoDoItem(intervalo, ultimaVezKm = 10_000, ultimaVezEm = feitoEm.toEpochMilli(), kmAtual, agora, fuso)

    @Test
    fun mudarOIntervaloRecalculaAProximaRevisao() {
        val antes = situacao(Intervalo(km = 5_000, dias = 180), kmAtual = 12_000)
        assertEquals(ProximaRevisao(15_000, LocalDate.of(2026, 7, 9)), antes.proxima)
        val depois = situacao(Intervalo(km = 3_000, dias = 90), kmAtual = 12_000)
        assertEquals(ProximaRevisao(13_000, LocalDate.of(2026, 4, 10)), depois.proxima)
        assertEquals(1_000, depois.situacao.kmRestantes)
        assertEquals(30, depois.situacao.diasRestantes)
    }

    @Test
    fun urgenciaPorKmOuPorTempo() {
        assertEquals(Urgencia.EM_DIA, situacao(Intervalo(5_000, 180), 12_000).urgencia)
        assertEquals(Urgencia.PROXIMO, situacao(Intervalo(5_000, 180), 14_600).urgencia)
        assertEquals(Urgencia.PROXIMO, situacao(Intervalo(null, 70), 10_000).urgencia)
        assertEquals(Urgencia.VENCIDO, situacao(Intervalo(5_000, null), 15_000).urgencia)
        assertEquals(Urgencia.VENCIDO, situacao(Intervalo(null, 60), 10_000).urgencia)
    }

    @Test
    fun kmDoItemMaiorQueOAtualNaoQuebraAConta() {
        assertEquals(5_000, situacao(Intervalo(5_000, null), kmAtual = 9_000).situacao.kmRestantes)
    }

    @Test
    fun todaCategoriaTemPlanoBaseComIntervalosValidos() {
        (CategoriaMoto.entries + listOf(null)).forEach { categoria ->
            val itens = planoBase(categoria)
            assertTrue(itens.isNotEmpty())
            itens.forEach { Intervalo(it.km, it.dias) }
            assertTrue(itens.any { it.nome == "Troca de óleo" })
        }
    }

    @Test
    fun todaOcorrenciaCriaAoMenosUmaInspecao() {
        (TipoOcorrencia.entries + listOf(null)).forEach { assertTrue(inspecoesDaOcorrencia(it).isNotEmpty()) }
    }

    @Test
    fun itemPrecisaDeNomeEDeUmIntervalo() {
        assertEquals(setOf(CampoItem.NOME, CampoItem.INTERVALO), validarItem(" ", "", ""))
        assertEquals(setOf(CampoItem.KM, CampoItem.DIAS), validarItem("Óleo", "0", "9999"))
        assertTrue(validarItem("Óleo", "5.000", "").isEmpty())
    }
}
