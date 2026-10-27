package com.pedrohrsm.diariodebordo.localizacao

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** GPS de mentira: devolve [resposta] ou lança [falha]. */
class FonteLocalizacaoFalsa(var resposta: Ponto? = null, var falha: Throwable? = null) : FonteLocalizacao {
    override suspend fun atual(): Ponto {
        falha?.let { throw it }
        return checkNotNull(resposta) { "Sem resposta configurada" }
    }

    override fun atualizacoes(intervaloMs: Long, distanciaMinimaM: Float): Flow<Ponto> = emptyFlow()
}
