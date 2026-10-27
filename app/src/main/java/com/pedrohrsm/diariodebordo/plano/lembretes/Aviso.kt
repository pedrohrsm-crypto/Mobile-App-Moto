package com.pedrohrsm.diariodebordo.plano.lembretes

import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.pendencia
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import com.pedrohrsm.diariodebordo.plano.dominio.deveAvisar
import com.pedrohrsm.diariodebordo.plano.dominio.situacao
import java.time.Clock

data class Aviso(
    val itemId: String,
    val motoId: String,
    val nomeMoto: String,
    val nomeItem: String,
    val urgencia: Urgencia,
)

/**
 * Itens periódicos que vencem logo ou já venceram e ainda não foram avisados nesta urgência. Cada urgência
 * avisa uma vez (PROXIMO e depois VENCIDO, regra em [deveAvisar]); o aviso recomeça quando o item é feito
 * ou o intervalo muda. Itens de moto que não está na garagem (excluída) ficam de fora.
 */
fun avisosPendentes(
    itens: List<ItemDoPlano>,
    kmPorMoto: Map<String, Int>,
    nomesDasMotos: Map<String, String>,
    relogio: Clock,
): List<Aviso> = itens.mapNotNull { item ->
    if (item.pendencia) return@mapNotNull null
    val nomeMoto = nomesDasMotos[item.motoId] ?: return@mapNotNull null
    val situacao = item.situacao(kmPorMoto[item.motoId], relogio) ?: return@mapNotNull null
    if (!deveAvisar(situacao.urgencia, item.avisadoComo)) return@mapNotNull null
    Aviso(item.id, item.motoId, nomeMoto, item.nome, situacao.urgencia)
}.sortedByDescending { it.urgencia }
