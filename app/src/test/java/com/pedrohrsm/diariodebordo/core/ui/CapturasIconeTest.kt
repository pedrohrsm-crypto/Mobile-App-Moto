package com.pedrohrsm.diariodebordo.core.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Prova visual do ícone novo (#84): o vetor do foreground sobre o fundo grafite em 48 dp (tamanho comum de lista e de
 * launcher) e em 108 dp (tela cheia do adaptativo), e a versão monocromática (uma cor só, como o Android 13+ tinge o
 * `<monochrome>`). O Roborazzi não renderiza o `<adaptive-icon>` (o `painterResource` só aceita vetor e bitmap),
 * então a captura usa o vetor do foreground num `Image`; o recorte pela máscara do fabricante é do launcher.
 * Gravar: `./gradlew recordRoborazziDebug`; conferir: `./gradlew verifyRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class CapturasIconeTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun capturaIconeEm48e108Dp() {
        rule.setContent {
            DiarioTheme(tema = TemaDiario.Oficina, modoOficina = false) {
                Row(
                    Modifier.testTag(ICONES).background(MaterialTheme.colorScheme.background).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icone(48.dp, monocromatico = false)
                    Icone(108.dp, monocromatico = false)
                    Icone(48.dp, monocromatico = true)
                }
            }
        }
        rule.onNodeWithTag(ICONES).captureRoboImage("src/test/snapshots/icone/icone-48-108-dp.png")
    }

    @Composable
    private fun Icone(lado: Dp, monocromatico: Boolean) {
        val fundo = when {
            monocromatico -> MaterialTheme.colorScheme.surfaceVariant
            else -> colorResource(R.color.ic_launcher_background)
        }
        Box(Modifier.size(lado).background(fundo)) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(lado),
                colorFilter = if (monocromatico) ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant) else null,
            )
        }
    }

    private companion object {
        const val ICONES = "icones"
    }
}
