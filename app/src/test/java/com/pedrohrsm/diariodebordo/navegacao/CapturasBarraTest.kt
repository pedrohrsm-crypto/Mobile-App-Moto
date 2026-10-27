package com.pedrohrsm.diariodebordo.navegacao

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Guarda da barra inferior (revisão do PR #51): com 4 destinos em 360 dp, o rótulo "Garagem" quebrava em duas
 * linhas no modo oficina com fonte em 200%. Captura a barra nos 3 temas × modo oficina × 140% e 200%,
 * com a Garagem aberta. Gravar: `./gradlew recordRoborazziDebug`; conferir: `./gradlew verifyRoborazziDebug`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class CapturasBarraTest(private val caso: Caso) {
    data class Caso(val tema: TemaDiario, val modoOficina: Boolean, val fonte: Float) {
        val arquivo: String
            get() = "src/test/snapshots/navegacao/barra/${tema.name.lowercase()}" +
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
                    Box(Modifier.testTag(BARRA)) { BarraDestinos(Rotas.GARAGEM, onEscolher = {}) }
                }
            }
        }
        rule.onNodeWithTag(BARRA).captureRoboImage(caso.arquivo)
    }

    companion object {
        private const val BARRA = "barra"

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun casos(): List<Array<Any>> = buildList {
            for (tema in TemaDiario.entries) {
                for (modoOficina in listOf(false, true)) {
                    for (fonte in listOf(1.4f, 2.0f)) add(arrayOf(Caso(tema, modoOficina, fonte)))
                }
            }
        }
    }
}
