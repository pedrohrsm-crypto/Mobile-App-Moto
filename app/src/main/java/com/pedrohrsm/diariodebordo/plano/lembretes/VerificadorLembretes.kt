package com.pedrohrsm.diariodebordo.plano.lembretes

import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Verifica o plano de todas as motos de uma vez (tarefa única do WorkManager) e avisa o que mudou de urgência
 * desde o último aviso. Só grava a urgência avisada quando o aparelho aceitou a notificação.
 * Falha do banco sobe como exceção: quem chama decide tentar de novo, nada vira "sem avisos".
 */
class VerificadorLembretes @Inject constructor(
    private val motos: MotoRepositorio,
    private val hodometro: HodometroRepositorio,
    private val plano: PlanoRepositorio,
    private val notificador: Notificador,
    private val relogio: Clock,
) {
    /** Devolve quantos avisos foram enviados. */
    suspend fun verificar(): Int {
        val nomes = motos.observarAtivos().first().associate { it.id to it.apelido }
        val kms = hodometro.observarKmAtuais().first().associate { it.motoId to it.km }
        var enviados = 0
        avisosPendentes(plano.abertos(), kms, nomes, relogio).forEach { aviso ->
            if (notificador.notificar(aviso)) {
                // Falso: o item saiu do plano enquanto o aviso era enviado; não há o que marcar nem o que contar.
                if (plano.marcarAvisado(aviso.itemId, aviso.urgencia)) enviados++
            }
        }
        return enviados
    }
}
