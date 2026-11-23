package com.pedrohrsm.diariodebordo.diario.fotos

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

/** O WorkManager de verdade (Robolectric): a varredura de órfãs fica uma só na fila, uma vez por dia, sem rede. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgendadorDaVarreduraTest {

    private val contexto = RuntimeEnvironment.getApplication()

    @Test
    fun varreduraDiariaFicaUmaSoNaFilaSemRedeEComBateria() {
        val agendador = AgendadorDaVarredura(contexto)
        agendador.agendarDiaria()
        agendador.agendarDiaria()

        val fila = WorkManager.getInstance(contexto).getWorkInfosForUniqueWork(VarreduraWorker.NOME_UNICO)
        val info = fila.get().single()
        assertTrue(info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.RUNNING)
        assertEquals(TimeUnit.DAYS.toMillis(1), info.periodicityInfo?.repeatIntervalMillis)
        assertEquals(NetworkType.NOT_REQUIRED, info.constraints.requiredNetworkType)
        assertTrue(info.constraints.requiresBatteryNotLow())
    }
}
