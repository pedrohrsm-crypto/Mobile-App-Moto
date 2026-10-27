package com.pedrohrsm.diariodebordo.plano.dominio

import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.intervalo
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Da menor para a maior: a ordem das constantes é a regra de [deveAvisar] e da ordenação da tela. */
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

/**
 * A situação de um item periódico agora, ou null se ele não tem intervalo (pendência). Única conversão do item
 * para [situacaoDoItem]: a tela e os lembretes usam esta. Sem leitura de hodômetro, conta do km da última vez.
 */
fun ItemDoPlano.situacao(kmAtual: Int?, relogio: Clock): SituacaoDoItem? {
    val intervalo = intervalo ?: return null
    val km = kmAtual ?: ultimaVezKm ?: 0
    return situacaoDoItem(
        intervalo = intervalo,
        ultimaVezKm = ultimaVezKm ?: km,
        ultimaVezEm = ultimaVezEm ?: criadoEm,
        kmAtual = km,
        agora = relogio.instant(),
        fuso = relogio.zone,
    )
}

/**
 * Regra única do aviso (erro #26): avisa quando a urgência de agora é maior que a última já avisada.
 * O que se guarda é "avisado em qual urgência", e não "já avisado": quem foi avisado como PROXIMO
 * ainda recebe o aviso de VENCIDO. Em dia nunca avisa.
 */
fun deveAvisar(urgencia: Urgencia, avisadoComo: Urgencia?): Boolean =
    urgencia != Urgencia.EM_DIA && (avisadoComo == null || urgencia > avisadoComo)
