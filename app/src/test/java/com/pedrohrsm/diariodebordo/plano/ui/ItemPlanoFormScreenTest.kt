package com.pedrohrsm.diariodebordo.plano.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Excluir um item do plano pede confirmação, como o diário: um toque errado não apaga o item. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ItemPlanoFormScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun texto(id: Int) = RuntimeEnvironment.getApplication().getString(id)

    private var exclusoes = 0

    private fun mostrar(editando: Boolean) {
        rule.setContent {
            DiarioTheme {
                ItemPlanoFormScreen(
                    ItemPlanoFormEstado(carregando = false, editando = editando, motoId = "1", nome = "Velas"),
                    onExcluir = { exclusoes++ },
                )
            }
        }
    }

    @Test
    fun excluirSoApagaDepoisDeConfirmar() {
        mostrar(editando = true)
        rule.onNodeWithText(texto(R.string.excluir)).performClick()
        rule.onNodeWithText(texto(R.string.item_plano_excluir_titulo)).assertIsDisplayed()
        assertEquals(0, exclusoes)
        // O botão de confirmar também se chama "Excluir": o que está no diálogo é o segundo.
        rule.onNodeWithText(texto(R.string.item_plano_excluir_texto)).assertIsDisplayed()
        rule.onAllNodesWithText(texto(R.string.excluir)).onLast().performClick()
        assertEquals(1, exclusoes)
    }

    @Test
    fun cancelarNaoApagaEFechaODialogo() {
        mostrar(editando = true)
        rule.onNodeWithText(texto(R.string.excluir)).performClick()
        rule.onNodeWithText(texto(R.string.cancelar)).performClick()
        assertEquals(0, exclusoes)
        rule.onNodeWithText(texto(R.string.item_plano_excluir_titulo)).assertDoesNotExist()
    }

    @Test
    fun itemNovoNaoTemBotaoDeExcluir() {
        mostrar(editando = false)
        rule.onNodeWithText(texto(R.string.excluir)).assertDoesNotExist()
    }
}
