package com.pedrohrsm.diariodebordo.plano.lembretes

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Verifica o plano de todas as motos de uma vez e avisa o que vence logo ou venceu.
 * Roda sem rede, só com o banco local. Regra em [avisosPendentes].
 */
@HiltWorker
class LembreteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val motos: MotoRepositorio,
    private val hodometro: HodometroRepositorio,
    private val plano: PlanoRepositorio,
    private val relogio: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val nomes = motos.observarAtivos().first().associate { it.id to it.apelido }
        val kms = hodometro.observarKmAtuais().first().associate { it.motoId to it.km }
        avisosPendentes(plano.abertos(), kms, nomes, relogio).forEach { aviso ->
            if (NotificadorLembretes.notificar(applicationContext, aviso)) {
                plano.marcarAvisado(aviso.itemId, relogio.millis())
            }
        }
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Banco indisponível no momento (atualização do app, por exemplo): tenta de novo mais tarde.
        if (runAttemptCount < TENTATIVAS) Result.retry() else Result.failure()
    }

    companion object {
        const val NOME_UNICO = "lembretes-manutencao"
        private const val TENTATIVAS = 3
    }
}
