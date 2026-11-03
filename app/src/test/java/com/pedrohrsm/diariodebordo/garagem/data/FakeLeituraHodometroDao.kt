package com.pedrohrsm.diariodebordo.garagem.data

import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Com [transacao], anota em [foraDaTransacao] cada leitura do km ou inserção feita fora de uma transação aberta. */
class FakeLeituraHodometroDao(private val transacao: TransacaoFalsa? = null) : LeituraHodometroDao {
    val linhas = MutableStateFlow<List<LeituraHodometro>>(emptyList())
    val foraDaTransacao = mutableListOf<String>()

    /** Quantos registros do diário da moto têm km (o DAO real conta na tabela `registros`). */
    var registrosComKm = 0

    private fun conferir(chamada: String) {
        if (transacao != null && !transacao.dentro) foraDaTransacao += chamada
    }

    private fun ativas(motoId: String) = linhas.value.filter { it.motoId == motoId && it.excluidoEm == null }

    override fun observarDaMoto(motoId: String): Flow<List<LeituraHodometro>> = linhas.map { _ ->
        ativas(motoId).sortedWith(compareByDescending<LeituraHodometro> { it.km }.thenByDescending { it.lidaEm })
    }

    override suspend fun kmAtual(motoId: String): Int? {
        conferir("kmAtual")
        return ativas(motoId).maxOfOrNull { it.km }
    }

    override fun observarKmAtual(motoId: String): Flow<Int?> = linhas.map { _ -> ativas(motoId).maxOfOrNull { it.km } }

    override fun observarKmAtuais(): Flow<List<KmAtual>> = linhas.map { todas ->
        todas.filter { it.excluidoEm == null }.groupBy { it.motoId }.map { (id, l) -> KmAtual(id, l.maxOf { it.km }) }
    }

    override suspend fun inserir(item: LeituraHodometro) {
        conferir("inserir")
        require(linhas.value.none { it.id == item.id }) { "id repetido: ${item.id}" }
        linhas.value = linhas.value + item
    }

    override suspend fun ativasDaMoto(motoId: String): List<LeituraHodometro> {
        conferir("ativasDaMoto")
        return ativas(motoId).sortedWith(compareBy<LeituraHodometro> { it.lidaEm }.thenBy { it.km }.thenBy { it.id })
    }

    override suspend fun buscar(id: String): LeituraHodometro? {
        conferir("buscar")
        return linhas.value.firstOrNull { it.id == id && it.excluidoEm == null }
    }

    override suspend fun marcarExcluida(id: String, quando: Long): Int {
        conferir("marcarExcluida")
        val alvo = linhas.value.firstOrNull { it.id == id && it.excluidoEm == null } ?: return 0
        linhas.value = linhas.value.map { if (it.id == id) alvo.copy(excluidoEm = quando, alteradoEm = quando) else it }
        return 1
    }

    override suspend fun registrosComKm(motoId: String): Int {
        conferir("registrosComKm")
        return registrosComKm
    }
}
