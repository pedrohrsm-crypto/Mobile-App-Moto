package com.pedrohrsm.diariodebordo.diario.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeRegistroDao : RegistroDao {
    val linhas = MutableStateFlow<List<Registro>>(emptyList())

    override fun observarDaMoto(motoId: String): Flow<List<Registro>> = linhas.map { todas ->
        todas.filter { it.motoId == motoId && it.excluidoEm == null }.sortedByDescending { it.dataEm }
    }

    override suspend fun buscar(id: String): Registro? = linhas.value.find { it.id == id && it.excluidoEm == null }

    override suspend fun inserir(item: Registro) {
        require(linhas.value.none { it.id == item.id }) { "id repetido: ${item.id}" }
        linhas.value = linhas.value + item
    }

    override suspend fun atualizar(item: Registro): Int {
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

class FakeFotoRegistroDao : FotoRegistroDao {
    val linhas = MutableStateFlow<List<FotoRegistro>>(emptyList())

    override suspend fun doRegistro(registroId: String): List<FotoRegistro> =
        linhas.value.filter { it.registroId == registroId && it.excluidoEm == null }.sortedBy { it.ordem }

    override suspend fun inserir(item: FotoRegistro) {
        linhas.value = linhas.value + item
    }

    override suspend fun atualizar(item: FotoRegistro): Int {
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
