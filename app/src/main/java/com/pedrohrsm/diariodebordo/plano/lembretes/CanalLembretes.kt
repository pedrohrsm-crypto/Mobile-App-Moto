package com.pedrohrsm.diariodebordo.plano.lembretes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.pedrohrsm.diariodebordo.R

object CanalLembretes {
    const val ID = "lembretes_manutencao"

    fun criar(context: Context) {
        val canal = NotificationChannel(
            ID,
            context.getString(R.string.canal_lembretes_nome),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }
}
