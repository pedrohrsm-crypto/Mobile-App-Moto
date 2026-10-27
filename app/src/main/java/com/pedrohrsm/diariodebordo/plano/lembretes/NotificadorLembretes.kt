package com.pedrohrsm.diariodebordo.plano.lembretes

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pedrohrsm.diariodebordo.MainActivity
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Mostra a notificação de um item do plano. Tocar abre a moto no item. */
object NotificadorLembretes {
    const val EXTRA_MOTO_ID = "lembrete_moto_id"
    const val EXTRA_ITEM_ID = "lembrete_item_id"

    fun podeNotificar(context: Context): Boolean {
        val permitido = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permitido && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Devolve false quando o aparelho não deixa notificar: o aviso fica para a próxima verificação. */
    @SuppressLint("MissingPermission") // conferido em podeNotificar, que cobre o Android 13+
    fun notificar(context: Context, aviso: Aviso): Boolean {
        if (!podeNotificar(context)) return false
        val abrir = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_MOTO_ID, aviso.motoId)
            putExtra(EXTRA_ITEM_ID, aviso.itemId)
        }
        val pendente = PendingIntent.getActivity(
            context,
            aviso.itemId.hashCode(),
            abrir,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val titulo = context.getString(
            if (aviso.urgencia == Urgencia.VENCIDO) R.string.lembrete_vencido else R.string.lembrete_proximo,
            aviso.nomeItem,
        )
        val notificacao = NotificationCompat.Builder(context, CanalLembretes.ID)
            .setSmallIcon(R.drawable.ic_plano)
            .setContentTitle(titulo)
            .setContentText(context.getString(R.string.lembrete_texto, aviso.nomeMoto))
            .setContentIntent(pendente)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(aviso.itemId.hashCode(), notificacao)
        return true
    }
}

/** O [Notificador] do app: notificação do sistema. */
class NotificadorDoSistema @Inject constructor(@ApplicationContext private val context: Context) : Notificador {
    override fun notificar(aviso: Aviso): Boolean = NotificadorLembretes.notificar(context, aviso)
}
