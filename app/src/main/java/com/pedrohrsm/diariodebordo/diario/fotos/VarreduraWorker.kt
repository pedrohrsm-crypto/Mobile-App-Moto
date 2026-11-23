package com.pedrohrsm.diariodebordo.diario.fotos

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Varredura diária das fotos órfãs (#94). Roda no WorkManager, fora da thread principal e longe da abertura do app;
 * a regra mora em [VarreduraDeFotosOrfas]. Com a restauração do backup em andamento, tenta de novo mais tarde.
 * Nenhum nome de arquivo sai daqui: só as contagens e o tipo do erro, guardados pelo WorkManager no aparelho.
 */
@HiltWorker
class VarreduraWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val varredura: VarreduraDeFotosOrfas,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        when (val resultado = withContext(Dispatchers.IO) { varredura.varrer() }) {
            VarreduraDeFotosOrfas.Resultado.Adiada -> Result.retry()
            is VarreduraDeFotosOrfas.Resultado.Feita -> Result.success(
                Data.Builder()
                    .putInt(CHAVE_APAGADAS, resultado.apagadas)
                    .putInt(CHAVE_FALHARAM, resultado.falharam)
                    .build(),
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Banco ou pasta ilegível: nada foi apagado. Tenta de novo; esgotadas as tentativas, fica o tipo do erro.
        if (runAttemptCount < TENTATIVAS) {
            Result.retry()
        } else {
            Result.failure(Data.Builder().putString(CHAVE_ERRO, e::class.java.simpleName).build())
        }
    }

    companion object {
        const val NOME_UNICO = "fotos-orfas"
        const val CHAVE_APAGADAS = "apagadas"
        const val CHAVE_FALHARAM = "falharam"
        const val CHAVE_ERRO = "erro"
        private const val TENTATIVAS = 3
    }
}

/** Agenda a [VarreduraWorker] uma vez por dia, sem rede e com a bateria fora do nível baixo. */
class AgendadorDaVarredura @Inject constructor(@ApplicationContext private val context: Context) {
    fun agendarDiaria() {
        val pedido = PeriodicWorkRequestBuilder<VarreduraWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(VarreduraWorker.NOME_UNICO, ExistingPeriodicWorkPolicy.KEEP, pedido)
    }
}
