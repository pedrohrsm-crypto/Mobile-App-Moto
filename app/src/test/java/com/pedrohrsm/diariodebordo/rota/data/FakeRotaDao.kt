package com.pedrohrsm.diariodebordo.rota.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeRotaDao : RotaDao {
    val linhas = MutableStateFlow<List<Rota>>(emptyList())

    private fun gravando() = linhas.value.filter {
        it.emGravacao && it.excluidoEm == null
    }.maxByOrNull { it.iniciadaEm }

    override fun observarSalvasDaMoto(motoId: String): Flow<List<Rota>> = linhas.map { todas ->
        todas.filter {
            it.motoId == motoId && it.excluidoEm == null && !it.emGravacao
        }.sortedByDescending { it.iniciadaEm }
    }

    override fun observarEmGravacao(): Flow<Rota?> = linhas.map { gravando() }

    override suspend fun emGravacao(): Rota? = gravando()

    override fun observar(id: String): Flow<Rota?> = linhas.map { todas ->
        todas.find {
            it.id == id &&
                it.excluidoEm == null
        }
    }

    override suspend fun buscar(id: String): Rota? = linhas.value.find { it.id == id && it.excluidoEm == null }

    override suspend fun inserir(item: Rota) {
        require(linhas.value.none { it.id == item.id }) { "id repetido: ${item.id}" }
        linhas.value = linhas.value + item
    }

    override suspend fun atualizar(item: Rota): Int {
        if (linhas.value.none { it.id == item.id }) return 0
        linhas.value = linhas.value.map { if (it.id == item.id) item else it }
        return 1
    }

    override suspend fun marcarExcluido(id: String, quando: Long): Int {
        val alvo = linhas.value.find { it.id == id && it.excluidoEm == null } ?: return 0
        // Igual à consulta do Room: a exclusão lógica também apaga o traçado (risco #23).
        linhas.value = linhas.value.map {
            if (it === alvo) it.copy(excluidoEm = quando, alteradoEm = quando, polilinha = "") else it
        }
        return 1
    }
}

class FakePontoDeRotaDao : PontoDeRotaDao {
    val linhas = MutableStateFlow<List<PontoDeRota>>(emptyList())

    /** Simula um DELETE que apaga menos do que devia: o apagar deixa este tanto de linhas para trás. */
    var deixarParaTras = 0

    override suspend fun daRota(rotaId: String): List<PontoDeRota> =
        linhas.value.filter { it.rotaId == rotaId }.sortedBy { it.instante }

    override fun observarQuantidade(rotaId: String): Flow<Int> = linhas.map { todas ->
        todas.count { it.rotaId == rotaId }
    }

    override suspend fun quantidade(rotaId: String): Int = linhas.value.count { it.rotaId == rotaId }

    override suspend fun inserir(item: PontoDeRota) {
        linhas.value = linhas.value + item
    }

    override suspend fun apagarDaRota(rotaId: String): Int {
        val antes = linhas.value.size
        val daRota = linhas.value.filter { it.rotaId == rotaId }
        val ficam = daRota.take(deixarParaTras).toSet()
        linhas.value = linhas.value.filter { it.rotaId != rotaId || it in ficam }
        return antes - linhas.value.size
    }
}
