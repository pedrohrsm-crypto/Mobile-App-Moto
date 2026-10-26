package com.pedrohrsm.diariodebordo.plano.lembretes

import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.OrigemItem
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class AvisosTest {

    private val agora = Instant.parse("2026-10-01T12:00:00Z")
    private val relogio = Clock.fixed(agora, ZoneId.of("America/Sao_Paulo"))
    private val nomes = mapOf("moto" to "Bandida")

    private fun item(nome: String, km: Int?, dias: Int?, avisadoEm: Long? = null) = ItemDoPlano(
        motoId = "moto",
        nome = nome,
        intervaloKm = km,
        intervaloDias = dias,
        ultimaVezKm = 10_000,
        ultimaVezEm = agora.minusSeconds(100L * 24 * 3600).toEpochMilli(),
        avisadoEm = avisadoEm,
    )

    @Test
    fun avisaVencidosPrimeiroDepoisProximosENuncaEmDia() {
        val itens = listOf(
            item("Pneus", km = 20_000, dias = 110), // faltam 10 dias: vence logo
            item("Óleo", km = 3_000, dias = null), // 13.100 km: venceu
            item("Velas", km = 20_000, dias = 730), // em dia
        )
        val avisos = avisosPendentes(itens, mapOf("moto" to 13_100), nomes, relogio)
        assertEquals(
            listOf("Óleo" to Urgencia.VENCIDO, "Pneus" to Urgencia.PROXIMO),
            avisos.map {
                it.nomeItem to
                    it.urgencia
            },
        )
        assertEquals("Bandida", avisos.first().nomeMoto)
    }

    @Test
    fun jaAvisadoPendenciaEMotoExcluidaNaoAvisam() {
        val itens = listOf(
            item("Óleo", km = 3_000, dias = null, avisadoEm = 1L),
            ItemDoPlano(motoId = "moto", nome = "Conferir manetes", origem = OrigemItem.OCORRENCIA),
            item("Óleo", km = 3_000, dias = null).copy(motoId = "excluida"),
        )
        assertEquals(emptyList<Aviso>(), avisosPendentes(itens, mapOf("moto" to 13_100), nomes, relogio))
    }
}
