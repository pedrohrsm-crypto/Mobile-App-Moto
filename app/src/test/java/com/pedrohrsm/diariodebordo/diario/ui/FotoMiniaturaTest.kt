package com.pedrohrsm.diariodebordo.diario.ui

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Foto apagada ou corrompida: a miniatura sai do "abrindo" e mostra o quadro vazio, sem girar para sempre. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class FotoMiniaturaTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun arquivoQueNaoAbreViraQuadroVazioENaoSpinnerEterno() {
        rule.setContent { DiarioTheme { FotoMiniatura("nao-existe.jpg", "Foto 1 do registro", 96.dp) } }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodes(girando).fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithContentDescription("Foto 1 do registro").assertIsDisplayed()
    }

    private val girando = SemanticsMatcher.expectValue(
        SemanticsProperties.ProgressBarRangeInfo,
        ProgressBarRangeInfo.Indeterminate,
    )
}
