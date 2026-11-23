package com.pedrohrsm.diariodebordo

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.pedrohrsm.diariodebordo.diario.fotos.AgendadorDaVarredura
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorLembretes
import com.pedrohrsm.diariodebordo.plano.lembretes.CanalLembretes
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DiarioApp :
    Application(),
    Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var lembretes: AgendadorLembretes

    @Inject
    lateinit var varredura: AgendadorDaVarredura

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        CanalLembretes.criar(this)
        lembretes.agendarDiario()
        varredura.agendarDiaria()
    }
}
