package com.pedrohrsm.diariodebordo.localizacao

import kotlinx.coroutines.flow.Flow

/**
 * De onde vem a posição. O GPS não precisa de internet: funciona em modo avião.
 * Interface para as telas e o serviço de rota serem testados sem aparelho.
 */
interface FonteLocalizacao {
    /** Posição atual ou [LocalIndisponivelException]; [SemPermissaoDeLocalizacaoException] sem permissão. */
    suspend fun atual(): Ponto

    /** Posições contínuas enquanto alguém coleta. Intervalo e distância mínima poupam bateria. */
    fun atualizacoes(intervaloMs: Long, distanciaMinimaM: Float): Flow<Ponto>
}
