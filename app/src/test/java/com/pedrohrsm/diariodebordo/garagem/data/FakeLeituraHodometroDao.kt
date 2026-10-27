package com.pedrohrsm.diariodebordo.garagem.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeLeituraHodometroDao : LeituraHodometroDao {
    val linhas = MutableStateFlow<List<LeituraHodometro>>(emptyList())

    private fun ativas(motoId: String) = linhas.value.filter { it.motoId == motoId && it.excluidoEm == null }

    override fun observarDaMoto(motoId: String): Flow<List<LeituraHodometro>> = linhas.map { _ ->
        ativas(motoId).sortedWith(compareByDescending<LeituraHodometro> { it.km }.thenByDescending { it.lidaEm })
    }

    override suspend fun kmAtual(motoId: String): Int? = ativas(motoId).maxOfOrNull { it.km }

    override fun observarKmAtuais(): Flow<List<KmAtual>> = linhas.map { todas ->
        todas.filter { it.excluidoEm == null }.groupBy { it.motoId }.map { (id, l) -> KmAtual(id, l.maxOf { it.km }) }
    }

    override suspend fun inserir(item: LeituraHodometro) {
        require(linhas.value.none { it.id == item.id }) { "id repetido: ${item.id}" }
        linhas.value = linhas.value + item
    }
}
