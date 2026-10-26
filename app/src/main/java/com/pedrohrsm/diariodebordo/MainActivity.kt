package com.pedrohrsm.diariodebordo

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
import androidx.compose.runtime.setValue
import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.escuro
import com.pedrohrsm.diariodebordo.core.designsystem.theme.tokens
import com.pedrohrsm.diariodebordo.navegacao.DiarioNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // O ajuste vive aqui, acima do tema: mudar o tema recompõe, não recria a Activity.
            var ajuste by rememberSaveable(stateSaver = AjusteTema.Saver) { mutableStateOf(AjusteTema()) }
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
                DiarioNavHost(ajuste = ajuste, aoAjustarTema = { ajuste = it })
            }
        }
    }
}
