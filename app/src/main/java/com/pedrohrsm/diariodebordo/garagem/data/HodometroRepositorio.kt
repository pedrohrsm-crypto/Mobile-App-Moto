package com.pedrohrsm.diariodebordo.garagem.data

import com.pedrohrsm.diariodebordo.core.database.Transacao
import com.pedrohrsm.diariodebordo.garagem.dominio.LeituraRecusadaException
import com.pedrohrsm.diariodebordo.garagem.dominio.ValidacaoLeitura
import com.pedrohrsm.diariodebordo.garagem.dominio.validarLeitura
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Único lugar que escreve leituras de hodômetro. Toda leitura passa por [validarLeitura], e a leitura do
 * km atual e a inserção ficam na mesma transação: duas gravações ao mesmo tempo não fazem o km voltar.
 */
class HodometroRepositorio @Inject constructor(
    private val dao: LeituraHodometroDao,
    private val transacao: Transacao,
    private val relogio: Clock,
) {
    fun observarDaMoto(motoId: String): Flow<List<LeituraHodometro>> = dao.observarDaMoto(motoId)

    fun observarKmAtuais(): Flow<List<KmAtual>> = dao.observarKmAtuais()

    suspend fun kmAtual(motoId: String): Int? = dao.kmAtual(motoId)

    /** Grava a leitura ou lança [LeituraRecusadaException] se o km não avança ou é inválido. */
    suspend fun registrar(motoId: String, km: Int, lidaEm: Long = relogio.millis()): LeituraHodometro =
        transacao.executar {
            val validacao = validarLeitura(dao.kmAtual(motoId), km)
            if (validacao != ValidacaoLeitura.Aceita) throw LeituraRecusadaException(validacao)
            gravar(motoId, km, lidaEm)
        }

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
    }

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
