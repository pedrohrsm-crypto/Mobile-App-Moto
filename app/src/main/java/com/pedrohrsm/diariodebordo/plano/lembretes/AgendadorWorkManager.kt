package com.pedrohrsm.diariodebordo.plano.lembretes

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * O WorkManager guarda as tarefas no próprio banco e as reagenda depois de reiniciar o aparelho,
 * então o lembrete dispara com o app fechado. Nenhuma tarefa precisa de rede.
 */
class AgendadorWorkManager @Inject constructor(@ApplicationContext private val context: Context) :
    AgendadorLembretes {

    override fun agendarDiario() {
        val pedido = PeriodicWorkRequestBuilder<LembreteWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(LembreteWorker.NOME_UNICO, ExistingPeriodicWorkPolicy.KEEP, pedido)
    }

    override fun recalcularAgora() {
        // REPLACE junta várias mudanças seguidas numa tarefa só (recálculo em lote). O atraso curto deixa a
        // transação que gravou o km terminar antes da leitura e agrupa leituras em sequência.
        val pedido = OneTimeWorkRequestBuilder<LembreteWorker>()
            .setInitialDelay(ATRASO_DO_RECALCULO_SEGUNDOS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOME_RECALCULO, ExistingWorkPolicy.REPLACE, pedido)
    }

    companion object {
        const val NOME_RECALCULO = "lembretes-recalculo"
        const val ATRASO_DO_RECALCULO_SEGUNDOS = 5L
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LembretesModule {
    @Binds
    abstract fun bindAgendador(impl: AgendadorWorkManager): AgendadorLembretes

    @Binds
    abstract fun bindNotificador(impl: NotificadorDoSistema): Notificador
}
