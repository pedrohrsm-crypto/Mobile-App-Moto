package com.pedrohrsm.diariodebordo.diario.data

import com.pedrohrsm.diariodebordo.core.database.Transacao
import com.pedrohrsm.diariodebordo.diario.dominio.MAXIMO_FOTOS
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Único lugar que escreve registros e fotos do diário. Salva o registro, as fotos e o avanço do
 * hodômetro numa transação só, e confere as linhas de toda atualização.
 */
class RegistroRepositorio @Inject constructor(
    private val dao: RegistroDao,
    private val fotos: FotoRegistroDao,
    private val hodometro: HodometroRepositorio,
    private val plano: PlanoRepositorio,
    private val transacao: Transacao,
    private val relogio: Clock,
) {
    fun observarDaMoto(motoId: String): Flow<List<Registro>> = dao.observarDaMoto(motoId)

    suspend fun buscar(id: String): Registro? = dao.buscar(id)

    suspend fun fotosDo(registroId: String): List<FotoRegistro> = fotos.doRegistro(registroId)

    /**
     * Cria ou atualiza o registro com as fotos na ordem dada (nomes de arquivo).
     * Devolve o registro salvo e os arquivos que deixaram de ser usados, para quem chamou apagar.
     */
    suspend fun salvar(registro: Registro, arquivosDasFotos: List<String>): Salvo {
        require(arquivosDasFotos.size <= MAXIMO_FOTOS) { "No máximo $MAXIMO_FOTOS fotos por registro" }
        return transacao.executar {
            val agora = relogio.millis()
            val existente = dao.buscar(registro.id)
            val salvo = if (existente == null) {
                registro.copy(
                    criadoEm = agora,
                    alteradoEm = agora,
                    fusoHorario = relogio.zone.id,
                    excluidoEm = null,
                    pendenteSync = false,
                ).also { dao.inserir(it) }
            } else {
                registro.copy(
                    criadoEm = existente.criadoEm,
                    fusoHorario = existente.fusoHorario,
                    alteradoEm = agora,
                ).also {
                    val linhas = dao.atualizar(it)
                    check(linhas == 1) { "Registro ${it.id} não foi atualizado ($linhas linhas)" }
                }
            }
            val removidos = sincronizarFotos(salvo.id, arquivosDasFotos, agora)
            salvo.hodometroKm?.let { hodometro.registrarSeAvancar(salvo.motoId, it, salvo.dataEm) }
            aplicarNoPlano(salvo)
            Salvo(salvo, removidos)
        }
    }

    /** Exclusão lógica do registro e das fotos. Devolve os arquivos para apagar do armazenamento. */
    suspend fun excluir(id: String): List<String> = transacao.executar {
        val agora = relogio.millis()
        val linhas = dao.marcarExcluido(id, agora)
        check(linhas == 1) { "Registro $id não foi excluído ($linhas linhas)" }
        fotos.doRegistro(id).map { foto ->
            check(fotos.marcarExcluido(foto.id, agora) == 1) { "Foto ${foto.id} não foi excluída" }
            foto.arquivo
        }
    }

    /** Manutenção de um item do plano o dá por feito; ocorrência cria as pendências de inspeção. */
    private suspend fun aplicarNoPlano(registro: Registro) {
        when (registro.tipo) {
            TipoRegistro.MANUTENCAO -> registro.itemPlanoId?.let { itemId ->
                val km = registro.hodometroKm ?: hodometro.kmAtual(registro.motoId) ?: return
                plano.registrarFeito(itemId, km, registro.dataEm, registro.id)
            }
            TipoRegistro.OCORRENCIA -> plano.criarPendencias(registro)
            else -> Unit
        }
    }

    private suspend fun sincronizarFotos(registroId: String, arquivos: List<String>, agora: Long): List<String> {
        val atuais = fotos.doRegistro(registroId)
        val removidos = atuais.filter { it.arquivo !in arquivos }
        removidos.forEach { foto ->
            check(fotos.marcarExcluido(foto.id, agora) == 1) { "Foto ${foto.id} não foi excluída" }
        }
        arquivos.forEachIndexed { ordem, arquivo ->
            val atual = atuais.find { it.arquivo == arquivo }
            if (atual == null) {
                fotos.inserir(
                    FotoRegistro(
                        registroId = registroId,
                        arquivo = arquivo,
                        ordem = ordem,
                        criadoEm = agora,
                        alteradoEm = agora,
                        fusoHorario = relogio.zone.id,
                    ),
                )
            } else if (atual.ordem != ordem) {
                check(fotos.atualizar(atual.copy(ordem = ordem, alteradoEm = agora)) == 1) {
                    "Foto ${atual.id} não foi atualizada"
                }
            }
        }
        return removidos.map { it.arquivo }
    }

    data class Salvo(val registro: Registro, val arquivosRemovidos: List<String>)
}
