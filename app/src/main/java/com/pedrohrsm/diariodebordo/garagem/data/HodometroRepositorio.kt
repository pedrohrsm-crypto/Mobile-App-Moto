package com.pedrohrsm.diariodebordo.garagem.data

import com.pedrohrsm.diariodebordo.core.database.Transacao
import com.pedrohrsm.diariodebordo.garagem.dominio.EdicaoRecusadaException
import com.pedrohrsm.diariodebordo.garagem.dominio.ExclusaoRecusadaException
import com.pedrohrsm.diariodebordo.garagem.dominio.LeituraRecusadaException
import com.pedrohrsm.diariodebordo.garagem.dominio.PontoDoHodometro
import com.pedrohrsm.diariodebordo.garagem.dominio.ValidacaoEdicao
import com.pedrohrsm.diariodebordo.garagem.dominio.ValidacaoExclusao
import com.pedrohrsm.diariodebordo.garagem.dominio.ValidacaoLeitura
import com.pedrohrsm.diariodebordo.garagem.dominio.validarEdicao
import com.pedrohrsm.diariodebordo.garagem.dominio.validarExclusao
import com.pedrohrsm.diariodebordo.garagem.dominio.validarLeitura
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorLembretes
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Único lugar que escreve leituras de hodômetro. Toda leitura passa por [validarLeitura], e a leitura do
 * km atual e a inserção ficam na mesma transação: duas gravações ao mesmo tempo não fazem o km voltar.
 * Cada leitura gravada pede um recálculo dos lembretes do plano (uma tarefa só por leitura, 1.4).
 */
class HodometroRepositorio @Inject constructor(
    private val dao: LeituraHodometroDao,
    private val transacao: Transacao,
    private val relogio: Clock,
    private val lembretes: AgendadorLembretes,
) {
    fun observarDaMoto(motoId: String): Flow<List<LeituraHodometro>> = dao.observarDaMoto(motoId)

    fun observarKmAtuais(): Flow<List<KmAtual>> = dao.observarKmAtuais()

    suspend fun kmAtual(motoId: String): Int? = dao.kmAtual(motoId)

    fun observarKmAtual(motoId: String): Flow<Int?> = dao.observarKmAtual(motoId)

    /** Grava a leitura ou lança [LeituraRecusadaException] se o km não avança ou é inválido. */
    suspend fun registrar(motoId: String, km: Int, lidaEm: Long = relogio.millis()): LeituraHodometro =
        transacao.executar {
            val validacao = validarLeitura(dao.kmAtual(motoId), km)
            if (validacao != ValidacaoLeitura.Aceita) throw LeituraRecusadaException(validacao)
            gravar(motoId, km, lidaEm)
        }.also { lembretes.recalcularAgora() }

    /**
     * Para registros do diário: grava a leitura só se ela avança o hodômetro.
     * Um registro com km antigo (lançamento retroativo) não mexe no km atual; km inválido é recusado.
     */
    suspend fun registrarSeAvancar(motoId: String, km: Int, lidaEm: Long): Boolean = transacao.executar {
        when (val validacao = validarLeitura(dao.kmAtual(motoId), km)) {
            ValidacaoLeitura.Aceita -> {
                gravar(motoId, km, lidaEm)
                true
            }
            is ValidacaoLeitura.MenorQueAnterior, is ValidacaoLeitura.IgualAnterior -> false
            ValidacaoLeitura.Negativa, ValidacaoLeitura.AcimaDoLimite -> throw LeituraRecusadaException(validacao)
        }
    }.also { avancou -> if (avancou) lembretes.recalcularAgora() }

    /**
     * Corrige o km de uma leitura já lançada: a antiga é marcada excluída e uma nova entra com a mesma data, tudo
     * na mesma transação (rastro para a sincronização, sem mudar o schema). Fora do intervalo das vizinhas pela
     * data lança [EdicaoRecusadaException]. Km igual ao atual não grava nada. Devolve a leitura que vale agora.
     */
    suspend fun corrigir(leituraId: String, novoKm: Int): LeituraHodometro {
        val corrigida = transacao.executar {
            val antiga = dao.buscar(leituraId) ?: throw EdicaoRecusadaException(ValidacaoEdicao.NaoEncontrada)
            val validacao = validarEdicao(dao.ativasDaMoto(antiga.motoId).map { it.ponto() }, leituraId, novoKm)
            if (validacao != ValidacaoEdicao.Aceita) throw EdicaoRecusadaException(validacao)
            if (antiga.km == novoKm) return@executar null
            val linhas = dao.marcarExcluida(leituraId, relogio.millis())
            check(linhas == 1) { "Leitura $leituraId não foi substituída ($linhas linhas)" }
            gravar(antiga.motoId, novoKm, antiga.lidaEm)
        }
        return corrigida?.also { lembretes.recalcularAgora() } ?: checkNotNull(dao.buscar(leituraId))
    }

    /**
     * Apaga uma leitura (exclusão lógica). As vizinhas continuam válidas entre si; só não se apaga a única leitura
     * de uma moto com registros do diário que têm km ([ExclusaoRecusadaException]).
     */
    suspend fun apagar(leituraId: String) {
        transacao.executar {
            val leitura = dao.buscar(leituraId) ?: throw ExclusaoRecusadaException(ValidacaoExclusao.NaoEncontrada)
            val validacao = validarExclusao(
                dao.ativasDaMoto(leitura.motoId).map { it.ponto() },
                leituraId,
                motoTemRegistrosComKm = dao.registrosComKm(leitura.motoId) > 0,
            )
            if (validacao != ValidacaoExclusao.Permitida) throw ExclusaoRecusadaException(validacao)
            val linhas = dao.marcarExcluida(leituraId, relogio.millis())
            check(linhas == 1) { "Leitura $leituraId não foi apagada ($linhas linhas)" }
        }
        lembretes.recalcularAgora()
    }

    private fun LeituraHodometro.ponto() = PontoDoHodometro(id, km, lidaEm, fusoHorario)

    private suspend fun gravar(motoId: String, km: Int, lidaEm: Long): LeituraHodometro {
        val agora = relogio.millis()
        val nova = LeituraHodometro(
            motoId = motoId,
            km = km,
            lidaEm = lidaEm,
            criadoEm = agora,
            alteradoEm = agora,
            fusoHorario = relogio.zone.id,
        )
        dao.inserir(nova)
        return nova
    }
}
