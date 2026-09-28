package com.pedrohrsm.diariodebordo.garagem.data

import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Único lugar que escreve em motos: preenche os campos de controle e confere as linhas afetadas. */
class MotoRepositorio @Inject constructor(private val dao: MotoDao, private val relogio: Clock) {
    fun observarAtivos(): Flow<List<Moto>> = dao.observarAtivos()

    fun observar(id: String): Flow<Moto?> = dao.observar(id)

    suspend fun criar(item: Moto): Moto {
        val agora = relogio.millis()
        val novo = item.copy(
            criadoEm = agora,
            alteradoEm = agora,
            fusoHorario = relogio.zone.id,
            excluidoEm = null,
            pendenteSync = false,
        )
        dao.inserir(novo)
        return novo
    }

    suspend fun atualizar(item: Moto): Moto {
        val alterado = item.copy(alteradoEm = relogio.millis())
        val linhas = dao.atualizar(alterado)
        check(linhas == 1) { "Moto ${item.id} não foi atualizada ($linhas linhas)" }
        return alterado
    }

    suspend fun excluir(id: String) {
        val linhas = dao.marcarExcluido(id, relogio.millis())
        check(linhas == 1) { "Moto $id não foi excluída ($linhas linhas)" }
    }
}
