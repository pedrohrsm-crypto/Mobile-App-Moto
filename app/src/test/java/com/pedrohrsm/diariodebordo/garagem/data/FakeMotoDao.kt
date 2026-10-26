package com.pedrohrsm.diariodebordo.garagem.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** DAO em memória para testes na JVM, sem Room nem emulador. */
class FakeMotoDao : MotoDao {
    val linhas = MutableStateFlow<List<Moto>>(emptyList())

    override fun observarAtivos(): Flow<List<Moto>> =
        linhas.map { todos -> todos.filter { it.excluidoEm == null }.sortedBy { it.apelido.lowercase() } }

    override fun observar(id: String): Flow<Moto?> =
        linhas.map { todos -> todos.find { it.id == id && it.excluidoEm == null } }

    override suspend fun inserir(item: Moto) {
        require(linhas.value.none { it.id == item.id }) { "id repetido: ${item.id}" }
        linhas.value = linhas.value + item
    }

    override suspend fun atualizar(item: Moto): Int {
        if (linhas.value.none { it.id == item.id }) return 0
        linhas.value = linhas.value.map { if (it.id == item.id) item else it }
        return 1
    }

    override suspend fun marcarExcluido(id: String, quando: Long): Int {
        val alvo = linhas.value.find { it.id == id && it.excluidoEm == null } ?: return 0
        linhas.value = linhas.value.map { if (it === alvo) it.copy(excluidoEm = quando, alteradoEm = quando) else it }
        return 1
    }
}
