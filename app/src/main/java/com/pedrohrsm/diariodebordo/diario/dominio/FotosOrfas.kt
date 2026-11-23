package com.pedrohrsm.diariodebordo.diario.dominio

import java.util.concurrent.TimeUnit

/** Um arquivo da pasta de fotos do app: o nome e a data da última modificação, em milissegundos (0 se desconhecida). */
data class ArquivoNaPasta(val nome: String, val modificadoEm: Long)

/**
 * Janela de carência da varredura de órfãs (#94): um arquivo mais novo que isto nunca sai, mesmo sem foto ativa.
 * A foto é gravada na pasta antes de o registro ser salvo (o formulário importa e só grava ao salvar) e a restauração
 * do backup grava os arquivos antes da transação. Dois dias cobrem um formulário esquecido aberto e qualquer
 * restauração, com folga: o custo de esperar é a foto excluída ficar até dois dias a mais no aparelho.
 */
val CARENCIA_DAS_ORFAS_MS: Long = TimeUnit.DAYS.toMillis(2)

/**
 * Regra única de órfão (#94, risco #79): sai da pasta o arquivo que nenhuma foto ativa (`excluidoEm` nulo) usa e
 * que foi modificado há pelo menos [carencia]. Na dúvida, fica: data desconhecida (0) ou no futuro (relógio do
 * aparelho voltou) conta como recente. Quem chama garante que [ativas] veio de uma leitura que deu certo: uma
 * leitura que falhou nunca pode virar conjunto vazio, que faria tudo sair.
 */
fun fotosOrfas(
    naPasta: List<ArquivoNaPasta>,
    ativas: Set<String>,
    agora: Long,
    carencia: Long = CARENCIA_DAS_ORFAS_MS,
): List<String> = naPasta
    .filter { it.nome !in ativas && it.modificadoEm > 0 && agora - it.modificadoEm >= carencia }
    .map { it.nome }
