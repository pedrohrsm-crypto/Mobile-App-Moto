package com.pedrohrsm.diariodebordo.core.database

import androidx.room.withTransaction

class TransacaoRoom(private val database: DiarioDatabase) : Transacao {
    override suspend fun <T> executar(bloco: suspend () -> T): T = database.withTransaction { bloco() }
}
