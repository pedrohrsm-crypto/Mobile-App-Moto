package com.pedrohrsm.diariodebordo.garagem.data

import com.pedrohrsm.diariodebordo.core.database.Transacao
import com.pedrohrsm.diariodebordo.diario.fotos.GuardaFotos
import com.pedrohrsm.diariodebordo.garagem.dominio.ContagemDaMoto
import com.pedrohrsm.diariodebordo.garagem.dominio.GravacaoEmAndamentoException
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorLembretes
import java.time.Clock
import javax.inject.Inject

/**
 * Exclui a moto e tudo o que é dela (#65): registros, fotos, plano, rotas e leituras recebem `excluidoEm` numa
 * transação só (exclusão lógica, docs/offline-first.md). Cada marcação confere as linhas com a contagem lida
 * na mesma transação; se uma não bate, nada fica excluído. Depois do commit, os arquivos das fotos saem do
 * armazenamento do app e os lembretes são recalculados.
 */
class ExclusaoDeMoto @Inject constructor(
    private val dao: ExclusaoDeMotoDao,
    private val transacao: Transacao,
    private val fotos: GuardaFotos,
    private val lembretes: AgendadorLembretes,
    private val relogio: Clock,
) {
    suspend fun contar(motoId: String): ContagemDaMoto = transacao.executar { lerContagem(motoId).first }

    /** Devolve o que foi excluído e os arquivos de foto que não saíram do armazenamento (órfãos, sem dado ligado). */
    suspend fun excluir(motoId: String): Resultado {
        val (contagem, arquivos) = transacao.executar {
            if (dao.contarRotasEmGravacao(motoId) > 0) throw GravacaoEmAndamentoException()
            val (lida, fotosDaMoto) = lerContagem(motoId)
            val agora = relogio.millis()
            confere("fotos", fotosDaMoto.size, dao.marcarFotos(motoId, agora))
            confere("registros", lida.registros, dao.marcarRegistros(motoId, agora))
            confere("itens do plano", lida.itensDoPlano, dao.marcarItensDoPlano(motoId, agora))
            confere("rotas", lida.rotas, dao.marcarRotas(motoId, agora))
            confere("leituras", lida.leituras, dao.marcarLeituras(motoId, agora))
            confere("moto", 1, dao.marcarMoto(motoId, agora))
            lida to fotosDaMoto
        }
        val ficaram = arquivos.filterNot { fotos.apagar(it) }
        lembretes.recalcularAgora()
        return Resultado(contagem, ficaram)
    }

    private suspend fun lerContagem(motoId: String): Pair<ContagemDaMoto, List<String>> {
        val arquivos = dao.arquivosDasFotos(motoId)
        val contagem = ContagemDaMoto(
            registros = dao.contarRegistros(motoId),
            fotos = arquivos.size,
            itensDoPlano = dao.contarItensDoPlano(motoId),
            rotas = dao.contarRotas(motoId),
            leituras = dao.contarLeituras(motoId),
        )
        return contagem to arquivos
    }

    private fun confere(oque: String, esperado: Int, linhas: Int) =
        check(linhas == esperado) { "Moto: $esperado $oque a excluir, $linhas linhas excluídas" }

    data class Resultado(val contagem: ContagemDaMoto, val fotosQueFicaram: List<String>)
}
