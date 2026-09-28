package com.pedrohrsm.diariodebordo.plano.dominio

import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.pendencia
import java.text.Normalizer

/** Nome do item nos planos-base; o atalho da moto procura por ele. */
const val NOME_TROCA_DE_OLEO = "Troca de óleo"

/**
 * Item de troca de óleo do plano da moto, para o atalho "registrar troca de óleo" (modo oficina, 1.8).
 * Aceita o nome com outra caixa ou sem acento ("troca de oleo") e com complemento ("Troca de óleo 10W40").
 */
fun itemDeTrocaDeOleo(itens: List<ItemDoPlano>): ItemDoPlano? {
    val alvo = semAcento(NOME_TROCA_DE_OLEO)
    val candidatos = itens.filter { !it.pendencia && it.excluidoEm == null }
    return candidatos.firstOrNull { semAcento(it.nome) == alvo }
        ?: candidatos.firstOrNull { semAcento(it.nome).startsWith(alvo) }
}

private fun semAcento(texto: String): String = Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")
