package com.pedrohrsm.diariodebordo

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.PapelCor
import com.pedrohrsm.diariodebordo.core.designsystem.theme.escuro
import com.pedrohrsm.diariodebordo.core.designsystem.theme.tokens
import com.pedrohrsm.diariodebordo.core.preferencias.Preferencias
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.navegacao.Abertura
import com.pedrohrsm.diariodebordo.navegacao.DiarioNavHost
import com.pedrohrsm.diariodebordo.navegacao.comoAbertura
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
        // Depois de recriar a Activity (rotação), a abertura já foi tratada: não abre o plano de novo.
        if (savedInstanceState == null) lerAbertura(intent)
        // Abertura a frio sem piscar: o ajuste salvo é lido antes do primeiro quadro, e a janela já pinta o fundo
        // do tema escolhido (o do tema de partida está em themes.xml, para antes do app carregar).
        val salvo = preferencias.ajusteSalvo()
        window.setBackgroundDrawable(ColorDrawable(fundoDaJanela(salvo)))
        setContent {
            // O ajuste vive aqui, acima do tema: a tela de ajustes grava nas preferências, e a troca recompõe,
            // sem recriar a Activity.
            val ajuste by preferencias.ajuste.collectAsStateWithLifecycle(salvo)
            // Os ícones das barras seguem o tema do app, não o modo escuro do sistema.
            val escuro = ajuste.tema.tokens().escuro()
            DisposableEffect(ajuste.tema) {
                window.setBackgroundDrawable(ColorDrawable(fundoDaJanela(ajuste)))
                onDispose { }
            }
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
        val pedida = intent.comoAbertura() ?: return
        motoAtual.selecionar(pedida.motoId)
        abertura.value = pedida
    }
}

/** Fundo da janela no tema escolhido; sem ajuste salvo é o `janela_fundo` de colors.xml. */
fun fundoDaJanela(ajuste: AjusteTema): Int = ajuste.tema.tokens().getValue(PapelCor.FUNDO).toArgb()
