package com.pedrohrsm.diariodebordo.plano.dominio

import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.pendencia

/** Chave estável do item de troca de óleo dos planos-base: não muda se a pessoa renomear o item. */
const val CHAVE_TROCA_DE_OLEO = "troca_de_oleo"

/** Nome com que o plano-base cria o item; é só texto de tela, a busca usa [CHAVE_TROCA_DE_OLEO]. */
const val NOME_TROCA_DE_OLEO = "Troca de óleo"

/**
 * Item de troca de óleo do plano da moto, para o atalho "registrar troca de óleo" (modo oficina, 1.8).
 * Procura pela chave estável, nunca pelo nome mostrado na tela: renomear ("Óleo 10W40") não perde o atalho,
 * e um item da pessoa que só se chama "Troca de óleo" não é confundido com o do plano-base.
 */
fun itemDeTrocaDeOleo(itens: List<ItemDoPlano>): ItemDoPlano? =
    itens.firstOrNull { it.chave == CHAVE_TROCA_DE_OLEO && !it.pendencia && it.excluidoEm == null }
