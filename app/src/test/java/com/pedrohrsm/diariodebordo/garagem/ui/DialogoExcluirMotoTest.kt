package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.garagem.dominio.ContagemDaMoto
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** O botão de excluir do diálogo só libera depois que a pessoa digita o apelido da moto (#65). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DialogoExcluirMotoTest {

    @get:Rule
    val rule = createComposeRule()

    private val contagem = ContagemDaMoto(registros = 12, fotos = 1, itensDoPlano = 7, rotas = 3, leituras = 4)
    private var exclusoes = 0

    private fun mostrar(inicial: DialogoExclusao) = rule.setContent {
        var dialogo by remember { mutableStateOf(inicial) }
        DiarioTheme {
            DialogoExcluirMoto(
                dialogo = dialogo,
                onTexto = { dialogo = dialogo.copy(texto = it) },
                onExcluir = { exclusoes++ },
                onBackup = {},
                onFechar = {},
            )
        }
    }

    @Test
    fun botaoSoHabilitaDepoisDeDigitarOApelido() {
        mostrar(DialogoExclusao(apelido = "Bandida", contagem = contagem))
        val botao = rule.onNodeWithTag(TAG_BOTAO_EXCLUIR_MOTO)
        botao.assertIsNotEnabled()
        botao.performClick()
        assertEquals(0, exclusoes)

        rule.onNode(hasSetTextAction()).performTextInput("Band")
        botao.assertIsNotEnabled()

        rule.onNode(hasSetTextAction()).performTextReplacement("Bandida")
        botao.assertIsEnabled()
        botao.performClick()
        assertEquals(1, exclusoes)
    }

    @Test
    fun mostraAContagemDoQueVaiSumir() {
        mostrar(DialogoExclusao(apelido = "Bandida", contagem = contagem))
        rule.onNodeWithText("12 registros do diário").assertIsDisplayed()
        rule.onNodeWithText("1 foto").assertIsDisplayed()
        rule.onNodeWithText("7 itens do plano de manutenção").assertIsDisplayed()
        rule.onNodeWithText("3 rotas gravadas").assertIsDisplayed()
    }

    @Test
    fun enquantoContaOBotaoFicaDesabilitadoMesmoComOApelido() {
        mostrar(DialogoExclusao(apelido = "Bandida", contagem = null, texto = "Bandida"))
        rule.onNodeWithTag(TAG_BOTAO_EXCLUIR_MOTO).assertIsNotEnabled()
    }

    @Test
    fun mostraAsLeiturasDoHodometroQueVaoSumir() {
        mostrar(DialogoExclusao(apelido = "Bandida", contagem = contagem))
        rule.onNodeWithText("4 leituras do hodômetro").assertIsDisplayed()
    }

    @Test
    fun umaLeituraDoHodometroFicaNoSingular() {
        mostrar(DialogoExclusao(apelido = "Bandida", contagem = contagem.copy(leituras = 1)))
        rule.onNodeWithText("1 leitura do hodômetro").assertIsDisplayed()
    }

    @Test
    fun avisoDaExclusaoDizQuantasFotosFicaram() {
        rule.setContent { DiarioTheme { DialogoAvisoExclusao(AvisoExclusao(2, lembretesRecalculados = true), {}) } }
        rule.onNodeWithText("2 fotos não puderam ser apagadas", substring = true).assertIsDisplayed()
        rule.onNodeWithText("lembretes", substring = true).assertDoesNotExist()
    }

    @Test
    fun avisoSemFotosFalaSoDosLembretes() {
        rule.setContent { DiarioTheme { DialogoAvisoExclusao(AvisoExclusao(0, lembretesRecalculados = false), {}) } }
        rule.onNodeWithText("lembretes não foram recalculados", substring = true).assertIsDisplayed()
        rule.onNodeWithText("fotos", substring = true).assertDoesNotExist()
    }

    @Test
    fun okDoAvisoFechaParaVoltarAGaragem() {
        var fechou = 0
        rule.setContent { DiarioTheme { DialogoAvisoExclusao(AvisoExclusao(1, true), onFechar = { fechou++ }) } }
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, fechou)
    }
}
