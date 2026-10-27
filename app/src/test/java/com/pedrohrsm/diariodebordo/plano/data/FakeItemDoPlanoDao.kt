package com.pedrohrsm.diariodebordo.plano.data

import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeItemDoPlanoDao : ItemDoPlanoDao {
    val linhas = MutableStateFlow<List<ItemDoPlano>>(emptyList())

    /** O diário que a consulta `ocorrenciasDaMoto` lê. */
    val registros = MutableStateFlow<List<Registro>>(emptyList())

    /** Quando preenchido, toda leitura e escrita falha com ele: o banco indisponível. */
    var falha: Throwable? = null

    /** Quando preenchido, `atualizar` devolve este número de linhas sem gravar: o item some no meio da escrita. */
    var linhasDaAtualizacao: Int? = null

    private fun conferir() {
        falha?.let { throw it }
    }

    private fun abertos(motoId: String?) = linhas.value.filter {
        (motoId == null || it.motoId == motoId) && it.excluidoEm == null && it.resolvidoEm == null
    }

    override fun observarDaMoto(motoId: String): Flow<List<ItemDoPlano>> = linhas.map { _ ->
        conferir()
        abertos(motoId).sortedBy { it.nome.lowercase() }
    }

    override suspend fun abertos(): List<ItemDoPlano> {
        conferir()
        return abertos(null)
    }

    override suspend fun buscar(id: String): ItemDoPlano? {
        conferir()
        return linhas.value.find { it.id == id && it.excluidoEm == null }
    }

    override suspend fun contarDaOcorrencia(registroId: String): Int {
        conferir()
        return linhas.value.count { it.registroOrigemId == registroId && it.excluidoEm == null }
    }

    override suspend fun contarPlanoBase(motoId: String): Int {
        conferir()
        return linhas.value.count { it.motoId == motoId && it.origem == OrigemItem.PLANO_BASE }
    }

    override suspend fun ocorrenciasDaMoto(motoId: String): List<Registro> {
        conferir()
        return registros.value.filter {
            it.motoId == motoId && it.tipo == TipoRegistro.OCORRENCIA && it.excluidoEm == null
        }
    }

    override suspend fun inserir(item: ItemDoPlano) {
        conferir()
        require(linhas.value.none { it.id == item.id }) { "id repetido: ${item.id}" }
        linhas.value = linhas.value + item
    }

    override suspend fun atualizar(item: ItemDoPlano): Int {
        conferir()
        linhasDaAtualizacao?.let { return it }
        if (linhas.value.none { it.id == item.id }) return 0
        linhas.value = linhas.value.map { if (it.id == item.id) item else it }
        return 1
    }

    override suspend fun marcarAvisado(id: String, urgencia: Urgencia, quando: Long): Int {
        conferir()
        val alvo = linhas.value.find { it.id == id && it.excluidoEm == null && it.resolvidoEm == null } ?: return 0
        linhas.value = linhas.value.map {
            if (it === alvo) it.copy(avisadoComo = urgencia, alteradoEm = quando) else it
        }
        return 1
    }

    override suspend fun marcarExcluido(id: String, quando: Long): Int {
        conferir()
        val alvo = linhas.value.find { it.id == id && it.excluidoEm == null } ?: return 0
        linhas.value = linhas.value.map { if (it === alvo) it.copy(excluidoEm = quando, alteradoEm = quando) else it }
        return 1
    }
}
