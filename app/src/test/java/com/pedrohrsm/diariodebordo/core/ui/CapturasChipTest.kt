package com.pedrohrsm.diariodebordo.core.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
 * Guarda do chip de escolha (erro #55): o chip selecionado se distingue pela borda (`primary`, 2 dp) e o não
 * selecionado tem borda em `outline`. Captura a linha de chips com o 2º selecionado nos 3 temas × modo oficina × 140% e
 * 200%, em 360 dp. Gravar: `./gradlew recordRoborazziDebug`; conferir: `./gradlew verifyRoborazziDebug`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class CapturasChipTest(private val caso: Caso) {
    data class Caso(val tema: TemaDiario, val modoOficina: Boolean, val fonte: Float) {
        val arquivo: String
            get() = "src/test/snapshots/core-ui/chip/${tema.name.lowercase()}" +
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
                    Box(Modifier.testTag(CHIPS).background(MaterialTheme.colorScheme.background).padding(16.dp)) {
                        EscolhaUnica(
                            opcoes = listOf("Gasolina", "Etanol", "Diesel"),
                            selecionada = "Etanol",
                            rotulo = { it },
                            onEscolher = {},
                        )
                    }
                }
            }
        }
        rule.onNodeWithTag(CHIPS).captureRoboImage(caso.arquivo)
    }

    companion object {
        private const val CHIPS = "chips"

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
