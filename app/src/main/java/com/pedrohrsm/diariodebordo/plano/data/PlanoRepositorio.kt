package com.pedrohrsm.diariodebordo.plano.data

import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.plano.dominio.inspecoesDaOcorrencia
import com.pedrohrsm.diariodebordo.plano.dominio.planoBase
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Único lugar que escreve no plano de manutenção. Confere as linhas de toda atualização. */
class PlanoRepositorio @Inject constructor(private val dao: ItemDoPlanoDao, private val relogio: Clock) {

    fun observarDaMoto(motoId: String): Flow<List<ItemDoPlano>> = dao.observarDaMoto(motoId)

    suspend fun buscar(id: String): ItemDoPlano? = dao.buscar(id)

    suspend fun abertos(): List<ItemDoPlano> = dao.abertos()

    /** Plano-base da categoria, contando a partir do cadastro (km inicial e data de hoje). */
    suspend fun semearPlanoBase(moto: Moto, kmInicial: Int) {
        val agora = relogio.millis()
        planoBase(moto.categoria).forEach { base ->
            inserir(
                ItemDoPlano(
                    motoId = moto.id,
                    nome = base.nome,
                    intervaloKm = base.km,
                    intervaloDias = base.dias,
                    ultimaVezKm = kmInicial,
                    ultimaVezEm = agora,
                    origem = OrigemItem.PLANO_BASE,
                ),
            )
        }
    }

    /** Cria um item novo ou salva a edição (nome e intervalos). */
    suspend fun salvar(item: ItemDoPlano, kmAtual: Int?): ItemDoPlano {
        val existente = dao.buscar(item.id)
        if (existente == null) {
            val novo = item.copy(
                ultimaVezKm = item.ultimaVezKm ?: kmAtual ?: 0,
                ultimaVezEm =
                item.ultimaVezEm ?: relogio.millis(),
            )
            inserir(novo)
            return novo
        }
        val alterado = existente.copy(
            nome = item.nome,
            intervaloKm = item.intervaloKm,
            intervaloDias = item.intervaloDias,
            // Mudar o intervalo muda a próxima revisão: o aviso antigo deixa de valer.
            avisadoEm = null,
            alteradoEm = relogio.millis(),
        )
        atualizar(alterado)
        return alterado
    }

    /**
     * Uma manutenção do diário cumpriu o item. Só avança a referência: um registro mais antigo
     * que a última vez conhecida não a faz voltar.
     */
    suspend fun registrarFeito(itemId: String, km: Int, quando: Long, registroId: String) {
        val item = dao.buscar(itemId) ?: return
        val atual = item.ultimaVezEm
        if (item.ultimoRegistroId != null && atual != null && quando < atual) return
        atualizar(
            item.copy(
                ultimaVezKm = km,
                ultimaVezEm = quando,
                ultimoRegistroId = registroId,
                avisadoEm = null,
                alteradoEm = relogio.millis(),
            ),
        )
    }

    /** Uma ocorrência cria os itens de inspeção dela, uma vez só por registro. */
    suspend fun criarPendencias(ocorrencia: Registro) {
        if (dao.contarDaOcorrencia(ocorrencia.id) > 0) return
        inspecoesDaOcorrencia(ocorrencia.ocorrencia).forEach { nome ->
            inserir(
                ItemDoPlano(
                    motoId = ocorrencia.motoId,
                    nome = nome,
                    origem = OrigemItem.OCORRENCIA,
                    registroOrigemId = ocorrencia.id,
                ),
            )
        }
    }

    suspend fun resolverPendencia(id: String) {
        val item = checkNotNull(dao.buscar(id)) { "Pendência $id não existe" }
        val agora = relogio.millis()
        atualizar(item.copy(resolvidoEm = agora, alteradoEm = agora))
    }

    suspend fun marcarAvisado(id: String, quando: Long) {
        val item = dao.buscar(id) ?: return
        atualizar(item.copy(avisadoEm = quando, alteradoEm = relogio.millis()))
    }

    suspend fun excluir(id: String) {
        val linhas = dao.marcarExcluido(id, relogio.millis())
        check(linhas == 1) { "Item $id não foi excluído ($linhas linhas)" }
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
