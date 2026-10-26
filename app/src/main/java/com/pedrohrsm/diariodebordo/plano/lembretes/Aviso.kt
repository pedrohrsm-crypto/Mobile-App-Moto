package com.pedrohrsm.diariodebordo.plano.lembretes

import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.intervalo
import com.pedrohrsm.diariodebordo.plano.data.pendencia
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import com.pedrohrsm.diariodebordo.plano.dominio.situacaoDoItem
import java.time.Clock

data class Aviso(
    val itemId: String,
    val motoId: String,
    val nomeMoto: String,
    val nomeItem: String,
    val urgencia: Urgencia,
)

/**
 * Itens periódicos que vencem logo ou já venceram e ainda não foram avisados. Cada situação
 * avisa uma vez: o aviso volta a valer quando o item é feito ou o intervalo muda.
 * Itens de moto que não está na garagem (excluída) ficam de fora.
 */
fun avisosPendentes(
    itens: List<ItemDoPlano>,
    kmPorMoto: Map<String, Int>,
    nomesDasMotos: Map<String, String>,
    relogio: Clock,
): List<Aviso> = itens.mapNotNull { item ->
    if (item.pendencia || item.avisadoEm != null) return@mapNotNull null
    val intervalo = item.intervalo ?: return@mapNotNull null
    val nomeMoto = nomesDasMotos[item.motoId] ?: return@mapNotNull null
    val kmAtual = kmPorMoto[item.motoId] ?: item.ultimaVezKm ?: 0
    val situacao = situacaoDoItem(
        intervalo = intervalo,
        ultimaVezKm = item.ultimaVezKm ?: kmAtual,
        ultimaVezEm = item.ultimaVezEm ?: item.criadoEm,
        kmAtual = kmAtual,
        agora = relogio.instant(),
        fuso = relogio.zone,
    )
    if (situacao.urgencia == Urgencia.EM_DIA) return@mapNotNull null
    Aviso(item.id, item.motoId, nomeMoto, item.nome, situacao.urgencia)
}.sortedByDescending { it.urgencia }
