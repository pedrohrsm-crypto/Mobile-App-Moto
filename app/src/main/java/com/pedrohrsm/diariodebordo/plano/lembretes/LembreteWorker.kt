package com.pedrohrsm.diariodebordo.plano.lembretes

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Verifica o plano de manutenção e avisa o que venceu. Esqueleto: a regra vem na fase 1,
 * usando plano.dominio.situacaoDaManutencao. Roda sem rede, só com o banco local.
 */
@HiltWorker
class LembreteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = Result.success()

    companion object {
        const val NOME_UNICO = "lembretes-manutencao"
    }
}
