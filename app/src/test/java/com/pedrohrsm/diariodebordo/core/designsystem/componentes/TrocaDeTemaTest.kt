package com.pedrohrsm.diariodebordo.core.designsystem.componentes

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.PapelCor
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.tokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * L2 da frente fundacao-tema: o tema troca em tempo real. O teste toca no seletor de verdade, lê o pixel
 * que o Compose desenhou e confere que a Activity continua a mesma (nada foi recriado).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class TrocaDeTemaTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun corDaAmostra(): Color {
        val pixels = rule.onNodeWithTag(AMOSTRA).captureToImage().toPixelMap()
        return pixels[pixels.width / 2, pixels.height / 2]
    }

    private fun assertCor(esperada: Color, atual: Color, mensagem: String) =
        assertEquals(mensagem, "#%08x".format(esperada.toArgb()), "#%08x".format(atual.toArgb()))

    private fun montar(inicial: AjusteTema = AjusteTema()) {
        rule.setContent {
            var ajuste by androidx.compose.runtime.saveable.rememberSaveable(stateSaver = AjusteTema.Saver) {
                mutableStateOf(inicial)
            }
            DiarioTheme(tema = ajuste.tema, modoOficina = ajuste.modoOficina) {
                Column {
                    Box(Modifier.testTag(AMOSTRA).size(40.dp).background(MaterialTheme.colorScheme.primary))
                    SeletorTema(ajuste = ajuste, aoAjustar = { ajuste = it })
                }
            }
        }
    }

    @Test
    fun tocarNoTemaTrocaACorDoComponenteSemRecriarAActivity() {
        montar()
        val activity = rule.activity
        assertCor(TemaDiario.OFICINA.tokens().getValue(PapelCor.DESTAQUE), corDaAmostra(), "ponto de partida: Oficina")

        listOf("Estrada" to TemaDiario.ESTRADA, "Cromo" to TemaDiario.CROMO, "Oficina" to TemaDiario.OFICINA)
            .forEach { (rotulo, tema) ->
                rule.onNodeWithText(rotulo).performClick()
                rule.waitForIdle()
                assertCor(tema.tokens().getValue(PapelCor.DESTAQUE), corDaAmostra(), "depois de tocar em $rotulo")
                rule.onNodeWithText(rotulo).assertIsSelected()
                assertSame("a Activity foi recriada ao trocar para $rotulo", activity, rule.activity)
            }
    }

    @Test
    fun modoOficinaAumentaOAlvoDeToqueSemRecriarAActivity() {
        montar()
        val activity = rule.activity
        rule.onNodeWithText("Estrada").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("Modo oficina").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Estrada").assertHeightIsAtLeast(62.dp)
        assertSame(activity, rule.activity)
    }

    private companion object {
        const val AMOSTRA = "amostra"
    }
}
