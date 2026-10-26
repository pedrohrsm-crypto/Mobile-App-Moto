package com.pedrohrsm.diariodebordo.plano.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeItemDoPlanoDao : ItemDoPlanoDao {
    val linhas = MutableStateFlow<List<ItemDoPlano>>(emptyList())

    private fun abertos(motoId: String?) = linhas.value.filter {
        (motoId == null || it.motoId == motoId) && it.excluidoEm == null && it.resolvidoEm == null
    }

    override fun observarDaMoto(motoId: String): Flow<List<ItemDoPlano>> =
        linhas.map { _ -> abertos(motoId).sortedBy { it.nome.lowercase() } }

    override suspend fun abertos(): List<ItemDoPlano> = abertos(null)

    override suspend fun buscar(id: String): ItemDoPlano? = linhas.value.find { it.id == id && it.excluidoEm == null }

    override suspend fun contarDaOcorrencia(registroId: String): Int =
        linhas.value.count { it.registroOrigemId == registroId && it.excluidoEm == null }

    override suspend fun inserir(item: ItemDoPlano) {
        require(linhas.value.none { it.id == item.id }) { "id repetido: ${item.id}" }
        linhas.value = linhas.value + item
    }

    override suspend fun atualizar(item: ItemDoPlano): Int {
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
