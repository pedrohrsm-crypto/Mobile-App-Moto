package com.pedrohrsm.diariodebordo.diario.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.flow.MutableStateFlow

class FakeRegistroDao : RegistroDao {
    val linhas = MutableStateFlow<List<Registro>>(emptyList())

    /** O que a consulta paginada do Room devolveria, numa lista só: ativos da moto, mais recentes primeiro. */
    fun daMoto(motoId: String, tipo: String? = null): List<Registro> = linhas.value
        .filter { it.motoId == motoId && it.excluidoEm == null && (tipo == null || it.tipo.name == tipo) }
        .sortedWith(compareByDescending<Registro> { it.dataEm }.thenByDescending { it.criadoEm })

    /** Falha que a próxima página devolve, para os testes do estado de erro. */
    var falhaAoPaginar: Exception? = null

    override fun paginarDaMoto(motoId: String, tipo: String?): PagingSource<Int, Registro> =
        PaginasDeLista(daMoto(motoId, tipo)) { falhaAoPaginar }

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

/** PagingSource sobre uma lista fixa, com a chave sendo o índice do primeiro item da página. */
class PaginasDeLista<T : Any>(private val itens: List<T>, private val falha: () -> Exception? = { null }) :
    PagingSource<Int, T>() {
    override fun getRefreshKey(state: PagingState<Int, T>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        falha()?.let { return LoadResult.Error(it) }
        val inicio = params.key ?: 0
        val fim = minOf(inicio + params.loadSize, itens.size)
        return LoadResult.Page(itens.subList(inicio, fim), prevKey = null, nextKey = fim.takeIf { it < itens.size })
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
