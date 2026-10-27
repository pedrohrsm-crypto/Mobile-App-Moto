package com.pedrohrsm.diariodebordo.plano.lembretes

import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.OrigemItem
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import com.pedrohrsm.diariodebordo.plano.dominio.deveAvisar
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class AvisosTest {

    private val agora = Instant.parse("2026-10-01T12:00:00Z")
    private val relogio = Clock.fixed(agora, ZoneId.of("America/Sao_Paulo"))
    private val nomes = mapOf("moto" to "Bandida")

    private fun item(nome: String, km: Int?, dias: Int?, avisadoComo: Urgencia? = null) = ItemDoPlano(
        motoId = "moto",
        nome = nome,
        intervaloKm = km,
        intervaloDias = dias,
        ultimaVezKm = 10_000,
        ultimaVezEm = agora.minusSeconds(100L * 24 * 3600).toEpochMilli(),
        avisadoComo = avisadoComo,
    )

    private fun avisos(itens: List<ItemDoPlano>, km: Int) = avisosPendentes(itens, mapOf("moto" to km), nomes, relogio)

    @Test
    fun avisaVencidosPrimeiroDepoisProximosENuncaEmDia() {
        val itens = listOf(
            item("Pneus", km = 20_000, dias = 110), // faltam 10 dias: vence logo
            item("Óleo", km = 3_000, dias = null), // 13.100 km: venceu
            item("Velas", km = 20_000, dias = 730), // em dia
        )
        val avisos = avisos(itens, km = 13_100)
        assertEquals(
            listOf("Óleo" to Urgencia.VENCIDO, "Pneus" to Urgencia.PROXIMO),
            avisos.map { it.nomeItem to it.urgencia },
        )
        assertEquals("Bandida", avisos.first().nomeMoto)
    }

    @Test
    fun jaAvisadoNaMesmaUrgenciaPendenciaEMotoExcluidaNaoAvisam() {
        val itens = listOf(
            item("Óleo", km = 3_000, dias = null, avisadoComo = Urgencia.VENCIDO),
            item("Pneus", km = 20_000, dias = 110, avisadoComo = Urgencia.PROXIMO),
            ItemDoPlano(motoId = "moto", nome = "Conferir manetes", origem = OrigemItem.OCORRENCIA),
            item("Óleo", km = 3_000, dias = null).copy(motoId = "excluida"),
        )
        assertEquals(emptyList<Aviso>(), avisos(itens, km = 13_100))
    }

    @Test
    fun pendenciaNuncaAvisaNemSeAlguemLheDerUmIntervalo() {
        val pendencia = item("Conferir manetes", km = 3_000, dias = null).copy(origem = OrigemItem.OCORRENCIA)
        assertEquals(emptyList<Aviso>(), avisos(listOf(pendencia), km = 13_100))
    }

    /** Erro #26: o item avisado como PRÓXIMO nunca recebia o aviso de VENCIDO. */
    @Test
    fun proximoAvisadoVencidoAvisaDeNovo() {
        // 14.600 km: faltam 400 de 5.000, então só PRÓXIMO.
        val itemKm = item("Óleo", km = 5_000, dias = null)
        assertEquals(listOf(Urgencia.PROXIMO), avisos(listOf(itemKm), km = 14_600).map { it.urgencia })
        val avisadoComoProximo = itemKm.copy(avisadoComo = Urgencia.PROXIMO)
        assertEquals(emptyList<Aviso>(), avisos(listOf(avisadoComoProximo), km = 14_600))
        // Passou de 15.000 km: venceu, e o aviso de PRÓXIMO não cala o de VENCIDO.
        assertEquals(listOf(Urgencia.VENCIDO), avisos(listOf(avisadoComoProximo), km = 15_100).map { it.urgencia })
    }

    @Test
    fun proximoPorTempoAvisadoVencidoPorTempoAvisaDeNovo() {
        // Feito há 100 dias com intervalo de 110: faltam 10 (PRÓXIMO). Com intervalo de 100: venceu hoje.
        val proximo = item("Pneus", km = null, dias = 110, avisadoComo = Urgencia.PROXIMO)
        assertEquals(emptyList<Aviso>(), avisos(listOf(proximo), km = 10_000))
        val vencido = proximo.copy(intervaloDias = 100)
        assertEquals(listOf(Urgencia.VENCIDO), avisos(listOf(vencido), km = 10_000).map { it.urgencia })
    }

    /** A regra inteira, par a par: só sobe de urgência avisa, e EM_DIA nunca avisa. */
    @Test
    fun deveAvisarCobreTodasAsCombinacoesDeUrgenciaEAvisadoComo() {
        val esperado = mapOf(
            (Urgencia.EM_DIA to null) to false,
            (Urgencia.EM_DIA to Urgencia.EM_DIA) to false,
            (Urgencia.EM_DIA to Urgencia.PROXIMO) to false,
            (Urgencia.EM_DIA to Urgencia.VENCIDO) to false,
            (Urgencia.PROXIMO to null) to true,
            (Urgencia.PROXIMO to Urgencia.EM_DIA) to true,
            (Urgencia.PROXIMO to Urgencia.PROXIMO) to false,
            (Urgencia.PROXIMO to Urgencia.VENCIDO) to false,
            (Urgencia.VENCIDO to null) to true,
            (Urgencia.VENCIDO to Urgencia.EM_DIA) to true,
            (Urgencia.VENCIDO to Urgencia.PROXIMO) to true,
            (Urgencia.VENCIDO to Urgencia.VENCIDO) to false,
        )
        assertEquals(Urgencia.entries.size * (Urgencia.entries.size + 1), esperado.size)
        esperado.forEach { (par, avisa) ->
            assertEquals("avisadoComo=${par.second} urgência=${par.first}", avisa, deveAvisar(par.first, par.second))
        }
    }
}
