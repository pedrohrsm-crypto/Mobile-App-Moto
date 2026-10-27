package com.pedrohrsm.diariodebordo.plano.lembretes

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Tarefa do WorkManager que roda a verificação dos lembretes. Sem rede, só com o banco local.
 * A regra mora em [VerificadorLembretes] e em [avisosPendentes].
 */
@HiltWorker
class LembreteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val verificador: VerificadorLembretes,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        verificador.verificar()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Banco indisponível no momento (atualização do app, por exemplo): tenta de novo mais tarde.
        // Esgotadas as tentativas, a falha fica registrada no WorkManager com o tipo do erro (sem texto livre).
        if (runAttemptCount < TENTATIVAS) {
            Result.retry()
        } else {
            Result.failure(Data.Builder().putString(CHAVE_ERRO, e::class.java.simpleName).build())
        }
    }

    companion object {
        const val NOME_UNICO = "lembretes-manutencao"
        const val CHAVE_ERRO = "erro"
        private const val TENTATIVAS = 3
    }
}
