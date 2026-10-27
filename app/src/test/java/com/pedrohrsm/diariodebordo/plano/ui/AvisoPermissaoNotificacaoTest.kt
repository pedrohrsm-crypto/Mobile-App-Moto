package com.pedrohrsm.diariodebordo.plano.ui

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** A permissão de notificação (Android 13+) é pedida no plano, onde o lembrete faz sentido, e só a quem não deu. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AvisoPermissaoNotificacaoTest {

    @get:Rule
    val rule = createComposeRule()

    private val contexto = RuntimeEnvironment.getApplication()

    private fun mostrar() = rule.setContent { DiarioTheme { AvisoPermissaoNotificacao() } }

    @Test
    fun semPermissaoMostraOAvisoComOBotaoDeAtivar() {
        shadowOf(contexto).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        mostrar()
        rule.onNodeWithText(contexto.getString(R.string.lembrete_permissao)).assertIsDisplayed()
        rule.onNodeWithText(contexto.getString(R.string.lembrete_ativar)).assertIsDisplayed()
    }

    @Test
    fun comPermissaoNaoMostraNada() {
        shadowOf(contexto).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        mostrar()
        rule.onNodeWithText(contexto.getString(R.string.lembrete_permissao)).assertDoesNotExist()
        rule.onNodeWithText(contexto.getString(R.string.lembrete_ativar)).assertDoesNotExist()
    }
}
