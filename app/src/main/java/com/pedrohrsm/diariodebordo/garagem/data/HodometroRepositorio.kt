package com.pedrohrsm.diariodebordo.garagem.data

import com.pedrohrsm.diariodebordo.garagem.dominio.LeituraRecusadaException
import com.pedrohrsm.diariodebordo.garagem.dominio.ValidacaoLeitura
import com.pedrohrsm.diariodebordo.garagem.dominio.validarLeitura
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Único lugar que escreve leituras de hodômetro. Toda leitura passa por [validarLeitura]. */
class HodometroRepositorio @Inject constructor(private val dao: LeituraHodometroDao, private val relogio: Clock) {
    fun observarDaMoto(motoId: String): Flow<List<LeituraHodometro>> = dao.observarDaMoto(motoId)

    fun observarKmAtuais(): Flow<List<KmAtual>> = dao.observarKmAtuais()

    suspend fun kmAtual(motoId: String): Int? = dao.kmAtual(motoId)

    /** Grava a leitura ou lança [LeituraRecusadaException] se o km for menor ou igual ao atual. */
    suspend fun registrar(motoId: String, km: Int, lidaEm: Long = relogio.millis()): LeituraHodometro {
        val validacao = validarLeitura(dao.kmAtual(motoId), km)
        if (validacao != ValidacaoLeitura.Aceita) throw LeituraRecusadaException(validacao)
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

    /**
     * Para registros do diário: grava a leitura só se ela avança o hodômetro.
     * Um registro com km antigo (lançamento retroativo) não mexe no km atual.
     */
    suspend fun registrarSeAvancar(motoId: String, km: Int, lidaEm: Long): Boolean {
        val atual = dao.kmAtual(motoId)
        if (atual != null && km <= atual) return false
        registrar(motoId, km, lidaEm)
        return true
    }
}
