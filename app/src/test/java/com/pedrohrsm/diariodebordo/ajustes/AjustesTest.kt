package com.pedrohrsm.diariodebordo.ajustes

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.core.preferencias.FakePreferencias
import com.pedrohrsm.diariodebordo.core.premium.Premium
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tela de ajustes com o ViewModel de verdade: a escolha grava nas preferências, e o premium só aparece com a flag. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class AjustesTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun montar(preferencias: FakePreferencias, premium: Premium) {
        val viewModel = AjustesViewModel(preferencias, premium)
        rule.setContent {
            val ajuste by viewModel.ajuste.collectAsStateWithLifecycle()
            DiarioTheme(tema = ajuste.tema, modoOficina = ajuste.modoOficina) {
                AjustesScreen(ajuste, viewModel::ajustar, viewModel.recursosPremium)
            }
        }
    }

    @Test
    fun tocarNoTemaENoModoOficinaGravaNasPreferencias() {
        val preferencias = FakePreferencias()
        montar(preferencias, Premium(habilitado = false))
        rule.onNodeWithText("Estrada").performClick()
        rule.onNodeWithText("Modo oficina").performClick()
        rule.waitForIdle()
        assertEquals(AjusteTema(TemaDiario.ESTRADA, modoOficina = true), preferencias.ajusteSalvo())
        rule.onNodeWithText("Modo oficina").assertIsOn()
    }

    @Test
    fun premiumDesligadoNaoMostraNenhumRecursoPago() {
        montar(FakePreferencias(), Premium(habilitado = false))
        rule.onNodeWithText("Premium").assertDoesNotExist()
        rule.onNodeWithText("Motos sem limite").assertDoesNotExist()
        rule.onNodeWithText("Backup automático na nuvem").assertDoesNotExist()
    }

    @Test
    fun premiumLigadoMostraOsRecursos() {
        montar(FakePreferencias(), Premium(habilitado = true))
        rule.onNodeWithText("Premium").assertExists()
        rule.onNodeWithText("Motos sem limite").assertExists()
    }
}
