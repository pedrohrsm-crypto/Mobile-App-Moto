package com.pedrohrsm.diariodebordo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.core.preferencias.Preferencias
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

    @Inject
    lateinit var preferencias: Preferencias

    /** Aberto por uma notificação de lembrete: vai direto para a moto, no item do plano. */
    private val abertura = mutableStateOf<Abertura?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) lerAbertura(intent)
        setContent {
            val tema by preferencias.tema.collectAsStateWithLifecycle(TemaDiario.OFICINA)
            val modoOficina by preferencias.modoOficina.collectAsStateWithLifecycle(false)
            DiarioTheme(tema = tema, modoOficina = modoOficina) {
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
