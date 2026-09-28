package com.pedrohrsm.diariodebordo.backup.dominio

/** O que a importação vai gravar de uma tabela, e a contagem para mostrar à pessoa. */
data class Mesclagem<T>(val gravar: List<T>, val novos: Int, val atualizados: Int)

/**
 * Junta o que já está no aparelho com o que veio do arquivo: vale a alteração mais recente de cada
 * item pelo id (decisão de 26/09/2026). Empate fica com o do aparelho. Nada do aparelho é apagado.
 */
fun <T> mesclar(locais: List<T>, chegando: List<T>, id: (T) -> String, alteradoEm: (T) -> Long): Mesclagem<T> {
    val porId = locais.associateBy(id)
    val maisRecentes = chegando.groupBy(id).mapValues { (_, repetidos) -> repetidos.maxBy(alteradoEm) }.values
    var novos = 0
    var atualizados = 0
    val gravar = maisRecentes.filter { vindo ->
        val atual = porId[id(vindo)]
        when {
            atual == null -> true.also { novos++ }
            alteradoEm(vindo) > alteradoEm(atual) -> true.also { atualizados++ }
            else -> false
        }
    }
    return Mesclagem(gravar, novos, atualizados)
}
