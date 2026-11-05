package com.pedrohrsm.diariodebordo.plano.data

import com.pedrohrsm.diariodebordo.core.database.Transacao
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.plano.dominio.FeitoRecusadoException
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import com.pedrohrsm.diariodebordo.plano.dominio.acompanhaCorrecaoDoHodometro
import com.pedrohrsm.diariodebordo.plano.dominio.inspecoesDaOcorrencia
import com.pedrohrsm.diariodebordo.plano.dominio.planoBase
import com.pedrohrsm.diariodebordo.plano.dominio.validarFeito
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorLembretes
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Único lugar que escreve no plano de manutenção. Confere as linhas de toda atualização. */
class PlanoRepositorio @Inject constructor(
    private val dao: ItemDoPlanoDao,
    private val transacao: Transacao,
    private val relogio: Clock,
    private val lembretes: AgendadorLembretes,
) {

    fun observarDaMoto(motoId: String): Flow<List<ItemDoPlano>> = dao.observarDaMoto(motoId)

    suspend fun buscar(id: String): ItemDoPlano? = dao.buscar(id)

    suspend fun abertos(): List<ItemDoPlano> = dao.abertos()

    /**
     * Cria o plano-base da categoria na primeira vez que a moto abre o plano, contando de agora e do km atual.
     * Uma vez só por moto: quem apagou itens não os recebe de volta. Devolve quantos itens criou.
     */
    suspend fun garantirPlanoBase(moto: Moto, kmAtual: Int?): Int = transacao.executar {
        if (dao.contarPlanoBase(moto.id) > 0) return@executar 0
        val itens = planoBase(moto.categoria)
        itens.forEach { base ->
            inserir(
                ItemDoPlano(
                    motoId = moto.id,
                    chave = base.chave,
                    nome = base.nome,
                    intervaloKm = base.km,
                    intervaloDias = base.dias,
                    ultimaVezKm = kmAtual ?: 0,
                    ultimaVezEm = relogio.millis(),
                    origem = OrigemItem.PLANO_BASE,
                ),
            )
        }
        itens.size
    }

    /**
     * Cada ocorrência do diário da moto vira os itens de inspeção dela, uma vez só por ocorrência.
     * Idempotente: pode rodar a cada abertura do plano. Devolve quantos itens criou.
     */
    suspend fun sincronizarPendencias(motoId: String): Int = transacao.executar {
        var criados = 0
        dao.ocorrenciasDaMoto(motoId).forEach { ocorrencia ->
            if (dao.contarDaOcorrencia(ocorrencia.id) > 0) return@forEach
            inspecoesDaOcorrencia(ocorrencia.ocorrencia).forEach { nome ->
                inserir(
                    ItemDoPlano(
                        motoId = motoId,
                        nome = nome,
                        origem = OrigemItem.OCORRENCIA,
                        registroOrigemId = ocorrencia.id,
                    ),
                )
                criados++
            }
        }
        criados
    }

    /**
     * Cria um item novo ou salva a edição: nome, intervalos e o "feito" (km e instante da última vez, #69). O feito
     * editado passa por [validarFeito] (km entre 0 e o km atual, data até hoje) ou lança [FeitoRecusadoException].
     */
    suspend fun salvar(item: ItemDoPlano, kmAtual: Int?): ItemDoPlano {
        val existente = dao.buscar(item.id)
        if (existente == null) {
            val novo = item.copy(
                ultimaVezKm = item.ultimaVezKm ?: kmAtual ?: 0,
                ultimaVezEm = item.ultimaVezEm ?: relogio.millis(),
            )
            inserir(novo)
            lembretes.recalcularAgora()
            return novo
        }
        val feitoMudou = item.ultimaVezKm != existente.ultimaVezKm || item.ultimaVezEm != existente.ultimaVezEm
        if (feitoMudou) conferirFeito(item, kmAtual)
        val alterado = existente.copy(
            nome = item.nome,
            intervaloKm = item.intervaloKm,
            intervaloDias = item.intervaloDias,
            ultimaVezKm = item.ultimaVezKm,
            ultimaVezEm = item.ultimaVezEm,
            feitoEm = if (feitoMudou) item.ultimaVezEm else existente.feitoEm,
            // Mudar o intervalo ou o feito muda a próxima revisão: o aviso antigo deixa de valer.
            avisadoComo = null,
            alteradoEm = relogio.millis(),
        )
        atualizar(alterado)
        lembretes.recalcularAgora()
        return alterado
    }

    /** A pessoa fez o item agora: a conta recomeça do km atual e de hoje, e o aviso antigo deixa de valer. */
    suspend fun marcarFeito(itemId: String, kmAtual: Int?) {
        val item = checkNotNull(dao.buscar(itemId)) { "Item $itemId não existe" }
        val agora = relogio.millis()
        atualizar(
            item.copy(
                ultimaVezKm = kmAtual ?: item.ultimaVezKm ?: 0,
                ultimaVezEm = agora,
                feitoEm = agora,
                avisadoComo = null,
                alteradoEm = agora,
            ),
        )
    }

    /**
     * Correção de uma leitura do hodômetro (#69): os itens da moto cuja última vez é o km antigo passam ao km novo,
     * e o aviso antigo deixa de valer. Roda dentro da transação da correção: se um item não grava, a correção
     * inteira é desfeita. Os lembretes são recalculados por quem corrige. Devolve quantos itens mudaram.
     */
    suspend fun acompanharCorrecaoDoHodometro(motoId: String, kmAntigo: Int, kmNovo: Int): Int {
        val agora = relogio.millis()
        val itens = dao.ativosDaMoto(motoId).filter { acompanhaCorrecaoDoHodometro(it.ultimaVezKm, kmAntigo) }
        itens.forEach { atualizar(it.copy(ultimaVezKm = kmNovo, avisadoComo = null, alteradoEm = agora)) }
        return itens.size
    }

    suspend fun resolverPendencia(id: String) {
        val item = checkNotNull(dao.buscar(id)) { "Pendência $id não existe" }
        val agora = relogio.millis()
        atualizar(item.copy(resolvidoEm = agora, alteradoEm = agora))
    }

    /**
     * Grava a urgência já avisada. Falso quando o item saiu do plano (excluído ou resolvido) enquanto o aviso
     * era enviado: não há mais o que marcar, e não é falha.
     */
    suspend fun marcarAvisado(id: String, urgencia: Urgencia): Boolean =
        dao.marcarAvisado(id, urgencia, relogio.millis()) == 1

    suspend fun excluir(id: String) {
        val linhas = dao.marcarExcluido(id, relogio.millis())
        check(linhas == 1) { "Item $id não foi excluído ($linhas linhas)" }
    }

    private fun conferirFeito(item: ItemDoPlano, kmAtual: Int?) {
        val dia = Instant.ofEpochMilli(item.ultimaVezEm ?: relogio.millis()).atZone(relogio.zone).toLocalDate()
        val hoje = relogio.instant().atZone(relogio.zone).toLocalDate()
        val invalidos = validarFeito(item.ultimaVezKm, dia, kmAtual, hoje)
        if (invalidos.isNotEmpty()) throw FeitoRecusadoException(invalidos)
    }

    private suspend fun inserir(item: ItemDoPlano) {
        val agora = relogio.millis()
        dao.inserir(item.copy(criadoEm = agora, alteradoEm = agora, fusoHorario = relogio.zone.id))
    }

    private suspend fun atualizar(item: ItemDoPlano) {
        val linhas = dao.atualizar(item)
        check(linhas == 1) { "Item ${item.id} não foi atualizado ($linhas linhas)" }
    }
}
