package com.pedrohrsm.diariodebordo.core.database

/**
 * Agrupa escritas que só fazem sentido juntas (tudo ou nada). Interface para os testes na JVM
 * usarem uma versão que só executa o bloco; no app vale a [TransacaoRoom].
 */
interface Transacao {
    suspend fun <T> executar(bloco: suspend () -> T): T
}
