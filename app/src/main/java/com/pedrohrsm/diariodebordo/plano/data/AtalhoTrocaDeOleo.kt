package com.pedrohrsm.diariodebordo.plano.data

import com.pedrohrsm.diariodebordo.core.database.Transacao
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.RegistroRepositorio
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.plano.dominio.itemDeTrocaDeOleo
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Atalho do modo oficina (1.8): a troca de óleo feita agora, no km atual. Marca o item do plano como feito e
 * deixa a manutenção no diário, numa transação só. O item vem de [itemDeTrocaDeOleo], a regra única.
 */
class AtalhoTrocaDeOleo @Inject constructor(
    private val plano: PlanoRepositorio,
    private val registros: RegistroRepositorio,
    private val hodometro: HodometroRepositorio,
    private val transacao: Transacao,
    private val relogio: Clock,
) {
    suspend fun registrar(motoId: String): Registro = transacao.executar {
        val item = checkNotNull(itemDeTrocaDeOleo(plano.observarDaMoto(motoId).first())) {
            "Moto $motoId sem item de troca de óleo no plano"
        }
        val km = hodometro.kmAtual(motoId)
        plano.marcarFeito(item.id, km)
        val manutencao = Registro(
            motoId = motoId,
            tipo = TipoRegistro.MANUTENCAO,
            titulo = item.nome,
            dataEm = relogio.millis(),
            hodometroKm = km,
        )
        registros.salvar(manutencao, emptyList()).registro
    }
}
