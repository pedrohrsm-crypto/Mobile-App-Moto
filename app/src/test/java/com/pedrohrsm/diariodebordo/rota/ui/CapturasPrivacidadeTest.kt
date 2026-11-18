package com.pedrohrsm.diariodebordo.rota.ui

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.takahirom.roborazzi.captureRoboImage
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.rota.dominio.CorteDasPontas
import com.pedrohrsm.diariodebordo.rota.dominio.PontaDaRota
import com.pedrohrsm.diariodebordo.rota.dominio.ProtecaoDasRotas
import com.pedrohrsm.diariodebordo.rota.dominio.ZonaDePrivacidade
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Portão visual da privacidade das rotas (#78): a seção de Ajustes (corte das pontas), a lista de zonas e o
 * formulário da zona nova com o teclado aberto, em 360 dp, nos 3 temas, com e sem modo oficina, fonte em 140% e
 * 200%. O detalhe da rota com o trecho oculto em cinza está no [CapturasRotaTest]. Zonas inventadas, sem centro na tela.
 * Gravar: `./gradlew recordRoborazziDebug`; conferir: `./gradlew verifyRoborazziDebug`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class CapturasPrivacidadeTest(private val caso: Caso) {
    enum class Tela(val pasta: String, val teclado: Boolean = false) {
        SECAO_AJUSTES("privacidade-ajustes"),
        ZONAS("zonas"),
        ZONA_NOVA("zona-nova", teclado = true),
    }

    data class Caso(val tela: Tela, val tema: TemaDiario, val modoOficina: Boolean, val fonte: Float) {
        val arquivo: String
            get() = "src/test/snapshots/rota/${tela.pasta}/${tema.name.lowercase()}" +
                "-${if (modoOficina) "oficina" else "normal"}-fonte${(fonte * 100).toInt()}.png"

        override fun toString() = arquivo.removePrefix("src/test/snapshots/").removeSuffix(".png")
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun captura() {
        rule.setContent {
            val densidade = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(densidade.density, caso.fonte)) {
                DiarioTheme(tema = caso.tema, modoOficina = caso.modoOficina) {
                    Box(Modifier.fillMaxSize()) {
                        Tela(caso.tela)
                        // O teclado em si não é desenhado pela JVM: esta faixa ocupa o lugar dele na captura.
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .windowInsetsBottomHeight(WindowInsets.ime)
                                .background(MaterialTheme.colorScheme.inverseSurface),
                        )
                    }
                }
            }
        }
        if (caso.tela.teclado) abrirTeclado()
        rule.onRoot().captureRoboImage(caso.arquivo)
    }

    private fun abrirTeclado() {
        rule.runOnUiThread {
            val raiz = rule.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
            val altura = (ALTURA_TECLADO_DP * raiz.resources.displayMetrics.density).toInt()
            val ime = WindowInsetsCompat.Type.ime()
            val insets = WindowInsetsCompat.Builder()
                .setInsets(ime, Insets.of(0, 0, 0, altura))
                .setVisible(ime, true)
                .build()
            ViewCompat.dispatchApplyWindowInsets(raiz, insets)
        }
        rule.waitForIdle()
    }

    @Composable
    private fun Tela(tela: Tela) {
        when (tela) {
            Tela.SECAO_AJUSTES -> Surface(Modifier.fillMaxSize()) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(LocalEscalaDiario.current.espaco(16.dp)),
                ) {
                    SecaoPrivacidadeRotasConteudo(PrivacidadeEstado.Pronto(protecao))
                }
            }
            Tela.ZONAS -> ZonasScreen(PrivacidadeEstado.Pronto(protecao))
            Tela.ZONA_NOVA -> ZonaFormScreen(
                ZonaFormEstado(carregando = false, ponta = PontaDaRota.INICIO, nome = "Casa", raioM = 500),
            )
        }
    }

    companion object {
        private const val ALTURA_TECLADO_DP = 300

        private val protecao = ProtecaoDasRotas(
            CorteDasPontas.KM1,
            listOf(
                ZonaDePrivacidade(nome = "Casa", latitude = 0.0, longitude = 0.0, raioM = 500),
                ZonaDePrivacidade(nome = "Trabalho", latitude = 0.0, longitude = 0.0, raioM = 1_000),
            ),
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun casos(): List<Array<Any>> = buildList {
            for (tela in Tela.entries) {
                for (tema in TemaDiario.entries) {
                    for (modoOficina in listOf(false, true)) {
                        for (fonte in listOf(1.4f, 2.0f)) add(arrayOf(Caso(tela, tema, modoOficina, fonte)))
                    }
                }
            }
        }
    }
}
