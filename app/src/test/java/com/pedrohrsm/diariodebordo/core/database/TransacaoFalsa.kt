package com.pedrohrsm.diariodebordo.core.database

/**
 * Nos testes na JVM não há Room: a transação só executa o bloco, e conta quantas abriu e se há uma aberta
 * agora, para o DAO falso conferir que a leitura e a escrita aconteceram dentro dela. Aninha como o Room.
 */
class TransacaoFalsa : Transacao {
    var execucoes = 0
    private var abertas = 0
    val dentro: Boolean get() = abertas > 0

    override suspend fun <T> executar(bloco: suspend () -> T): T {
        execucoes++
        abertas++
        try {
            return bloco()
        } finally {
            abertas--
        }
    }
}
