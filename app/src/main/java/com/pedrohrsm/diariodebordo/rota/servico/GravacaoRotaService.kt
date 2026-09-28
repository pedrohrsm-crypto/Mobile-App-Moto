package com.pedrohrsm.diariodebordo.rota.servico

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.pedrohrsm.diariodebordo.MainActivity
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.localizacao.FonteLocalizacao
import com.pedrohrsm.diariodebordo.localizacao.PermissoesLocalizacao
import com.pedrohrsm.diariodebordo.rota.data.RotaRepositorio
import com.pedrohrsm.diariodebordo.rota.dominio.DISTANCIA_MINIMA_M
import com.pedrohrsm.diariodebordo.rota.dominio.INTERVALO_CAPTURA_MS
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Grava a rota num serviço em primeiro plano do tipo location, iniciado só pelo toque em "Gravar rota".
 * A notificação fixa mostra que o GPS está em uso e tem o botão Parar. Não usa localização em segundo
 * plano (ACCESS_BACKGROUND_LOCATION): o serviço visível mantém a captura com a tela desligada.
 * Cada ponto vai direto para o Room, então nada se perde se o app sair da tela.
 */
@AndroidEntryPoint
class GravacaoRotaService : Service() {

    @Inject
    lateinit var fonte: FonteLocalizacao

    @Inject
    lateinit var rotas: RotaRepositorio

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var coleta: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACAO_PARAR -> parar()
            // Sem ação = o sistema recriou o serviço (START_STICKY): retoma a rota que estava gravando.
            else -> iniciar(intent?.getStringExtra(EXTRA_MOTO_ID))
        }
        return START_STICKY
    }

    private fun iniciar(motoId: String?) {
        if (!PermissoesLocalizacao.temAlguma(this)) {
            falhar(getString(R.string.rota_sem_permissao))
            return
        }
        try {
            ServiceCompat.startForeground(
                this,
                ID_NOTIFICACAO,
                notificacao(getString(R.string.rota_gravando)),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } catch (e: IllegalStateException) {
            // Android 12+: o sistema recusa iniciar em primeiro plano quando o app não está visível.
            falhar(getString(R.string.rota_falha_gps, e.message.orEmpty()))
            return
        } catch (e: SecurityException) {
            falhar(getString(R.string.rota_sem_permissao))
            return
        }
        _gravando.value = true
        if (coleta?.isActive == true) return
        coleta = escopo.launch {
            try {
                val rota = rotas.emGravacao() ?: motoId?.let { rotas.iniciar(it) }
                if (rota == null) {
                    pararServico()
                    return@launch
                }
                fonte.atualizacoes(INTERVALO_CAPTURA_MS, DISTANCIA_MINIMA_M).collect { ponto ->
                    rotas.adicionarPonto(rota.id, ponto)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Os pontos já gravados ficam: a tela de rotas oferece salvar ou descartar a gravação.
                falhar(getString(R.string.rota_falha_gps, e.message.orEmpty()))
            }
        }
    }

    private fun parar() {
        val anterior = coleta
        escopo.launch {
            anterior?.cancelAndJoinSafe()
            runCatching { rotas.emGravacao()?.let { rotas.finalizar(it.id) } }
                .onFailure { _falha.value = it.message.orEmpty() }
            pararServico()
        }
    }

    private suspend fun Job.cancelAndJoinSafe() {
        cancel()
        join()
    }

    private fun falhar(mensagem: String) {
        _falha.value = mensagem
        pararServico()
    }

    private fun pararServico() {
        _gravando.value = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        _gravando.value = false
        escopo.cancel()
        super.onDestroy()
    }

    private fun notificacao(texto: String) = NotificationCompat.Builder(this, criarCanal(this))
        .setSmallIcon(R.drawable.ic_rotas)
        .setContentTitle(getString(R.string.rota_notificacao_titulo))
        .setContentText(texto)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .addAction(
            0,
            getString(R.string.rota_parar),
            PendingIntent.getService(
                this,
                1,
                Intent(this, GravacaoRotaService::class.java).setAction(ACAO_PARAR),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .build()

    companion object {
        private const val ACAO_PARAR = "parar"
        private const val EXTRA_MOTO_ID = "moto_id"
        private const val ID_NOTIFICACAO = 42
        private const val CANAL = "gravacao_rota"

        private val _gravando = MutableStateFlow(false)

        /** Se o serviço está capturando agora (a rota em gravação no banco pode ter sido interrompida). */
        val gravando: StateFlow<Boolean> = _gravando.asStateFlow()

        private val _falha = MutableStateFlow<String?>(null)
        val falha: StateFlow<String?> = _falha.asStateFlow()

        fun limparFalha() {
            _falha.value = null
        }

        fun iniciar(context: Context, motoId: String) {
            _falha.value = null
            ContextCompat.startForegroundService(
                context,
                Intent(context, GravacaoRotaService::class.java).putExtra(EXTRA_MOTO_ID, motoId),
            )
        }

        fun parar(context: Context) {
            context.startService(Intent(context, GravacaoRotaService::class.java).setAction(ACAO_PARAR))
        }

        private fun criarCanal(context: Context): String {
            val canal = NotificationChannel(
                CANAL,
                context.getString(R.string.rota_canal_nome),
                NotificationManager.IMPORTANCE_LOW,
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
            return CANAL
        }
    }
}
