package com.pedrohrsm.diariodebordo.core.database

/** Nos testes na JVM não há Room: a transação só executa o bloco. */
class TransacaoFalsa : Transacao {
    var execucoes = 0
    override suspend fun <T> executar(bloco: suspend () -> T): T {
        execucoes++
        return bloco()
    }
}
