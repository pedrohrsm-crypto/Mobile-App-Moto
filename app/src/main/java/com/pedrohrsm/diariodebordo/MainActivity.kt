package com.pedrohrsm.diariodebordo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.escuro
import com.pedrohrsm.diariodebordo.core.designsystem.theme.tokens
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.navegacao.Abertura
import com.pedrohrsm.diariodebordo.navegacao.DiarioNavHost
import com.pedrohrsm.diariodebordo.plano.lembretes.NotificadorLembretes
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var motoAtual: MotoAtual

    /** Aberto por uma notificação de lembrete: vai direto para a moto, no item do plano. */
    private val abertura = mutableStateOf<Abertura?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Depois de recriar a Activity (rotação), a abertura já foi tratada: não abre o plano de novo.
        if (savedInstanceState == null) lerAbertura(intent)
        setContent {
            // O ajuste vive aqui, acima do tema: mudar o tema recompõe, não recria a Activity.
            // A fatia de ajustes da Fase 1 liga o SeletorTema a este estado.
            val ajuste by rememberSaveable(stateSaver = AjusteTema.Saver) { mutableStateOf(AjusteTema()) }
            // Os ícones das barras seguem o tema do app, não o modo escuro do sistema.
            val escuro = ajuste.tema.tokens().escuro()
            DisposableEffect(escuro) {
                val barras = if (escuro) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barras, navigationBarStyle = barras)
                onDispose { }
            }
            DiarioTheme(tema = ajuste.tema, modoOficina = ajuste.modoOficina) {
                DiarioNavHost(abertura = abertura.value, onAberturaTratada = { abertura.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        lerAbertura(intent)
    }

    private fun lerAbertura(intent: Intent) {
        val motoId = intent.getStringExtra(NotificadorLembretes.EXTRA_MOTO_ID) ?: return
        motoAtual.selecionar(motoId)
        abertura.value = Abertura(motoId, intent.getStringExtra(NotificadorLembretes.EXTRA_ITEM_ID))
    }
}
