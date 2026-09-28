package com.pedrohrsm.diariodebordo.plano.dominio

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class Urgencia { EM_DIA, PROXIMO, VENCIDO }

/** Quando vence de novo: no km e/ou na data, o que chegar primeiro. */
data class ProximaRevisao(val km: Int?, val data: LocalDate?)

data class SituacaoDoItem(val situacao: Situacao, val proxima: ProximaRevisao, val urgencia: Urgencia)

/** Faltando até isto para vencer, o item já aparece como próximo. */
const val KM_AVISO_PREVIO = 500
const val DIAS_AVISO_PREVIO = 15

/**
 * Regra única do plano: a partir da última vez (km e instante) e do intervalo, diz quanto falta,
 * quando vence e a urgência. Usada pela tela e pelos lembretes.
 */
fun situacaoDoItem(
    intervalo: Intervalo,
    ultimaVezKm: Int,
    ultimaVezEm: Long,
    kmAtual: Int,
    agora: Instant,
    fuso: ZoneId,
): SituacaoDoItem {
    val ultimaData = Instant.ofEpochMilli(ultimaVezEm).atZone(fuso).toLocalDate()
    val hoje = agora.atZone(fuso).toLocalDate()
    val dias = ChronoUnit.DAYS.between(ultimaData, hoje).toInt().coerceAtLeast(0)
    // Um registro retroativo com km maior que o atual não quebra a conta: vale o maior.
    val situacao = situacaoDaManutencao(intervalo, ultimaVezKm, maxOf(kmAtual, ultimaVezKm), dias)
    val proxima = ProximaRevisao(
        km = intervalo.km?.let { ultimaVezKm + it },
        data = intervalo.dias?.let { ultimaData.plusDays(it.toLong()) },
    )
    val urgencia = when {
        situacao.vencida -> Urgencia.VENCIDO
        (situacao.kmRestantes ?: Int.MAX_VALUE) <= KM_AVISO_PREVIO ||
            (situacao.diasRestantes ?: Int.MAX_VALUE) <= DIAS_AVISO_PREVIO -> Urgencia.PROXIMO
        else -> Urgencia.EM_DIA
    }
    return SituacaoDoItem(situacao, proxima, urgencia)
}
