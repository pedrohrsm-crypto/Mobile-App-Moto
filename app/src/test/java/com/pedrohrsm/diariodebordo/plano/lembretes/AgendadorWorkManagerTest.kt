package com.pedrohrsm.diariodebordo.plano.lembretes

import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** O WorkManager de verdade (Robolectric): o que fica na fila, com que regra e sem depender de rede. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgendadorWorkManagerTest {

    private val contexto = RuntimeEnvironment.getApplication()
    private val agendador = AgendadorWorkManager(contexto)
    private val trabalho: WorkManager get() = WorkManager.getInstance(contexto)

    private fun fila(nome: String): List<WorkInfo> = trabalho.getWorkInfosForUniqueWork(nome).get()

    @Test
    fun verificacaoDiariaFicaUmaSoNaFilaSemRedeEUmaVezPorDia() {
        agendador.agendarDiario()
        agendador.agendarDiario()
        val info = fila(LembreteWorker.NOME_UNICO).single()
        // Na fila ou já rodando: o WorkManager do Robolectric dispara a primeira execução assim que agenda.
        assertTrue(info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.RUNNING)
        assertEquals(TimeUnit.DAYS.toMillis(1), info.periodicityInfo?.repeatIntervalMillis)
        assertEquals(NetworkType.NOT_REQUIRED, info.constraints.requiredNetworkType)
    }

    @Test
    fun variasLeiturasSeguidasDeixamUmaTarefaDeRecalculoSoNaFila() {
        repeat(3) { agendador.recalcularAgora() }
        val infos = fila(AgendadorWorkManager.NOME_RECALCULO)
        assertEquals(1, infos.count { it.state == WorkInfo.State.ENQUEUED })
        assertTrue(infos.all { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.CANCELLED })
        val naFila = infos.single { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(NetworkType.NOT_REQUIRED, naFila.constraints.requiredNetworkType)
    }
}
