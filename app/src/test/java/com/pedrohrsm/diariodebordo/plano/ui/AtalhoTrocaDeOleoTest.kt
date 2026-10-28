package com.pedrohrsm.diariodebordo.plano.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.preferencias.FakePreferencias
import com.pedrohrsm.diariodebordo.diario.data.FakeFotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.FakeRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.RegistroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.ui.MotoDetalheViewModel
import com.pedrohrsm.diariodebordo.plano.data.AtalhoTrocaDeOleo
import com.pedrohrsm.diariodebordo.plano.data.FakeItemDoPlanoDao
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import com.pedrohrsm.diariodebordo.plano.dominio.CHAVE_TROCA_DE_OLEO
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Troca de óleo em até 3 toques no modo oficina (1.8): o toque 1 abre a moto na garagem; aqui, dentro da moto,
 * o toque 2 é o atalho e o toque 3 confirma, e a troca fica gravada.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class AtalhoTrocaDeOleoTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val relogio = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val transacao = TransacaoFalsa()
    private val itensDao = FakeItemDoPlanoDao().apply {
        linhas.value =
            listOf(ItemDoPlano(id = "oleo", motoId = "m", chave = CHAVE_TROCA_DE_OLEO, nome = "Troca de óleo"))
    }
    private val registrosDao = FakeRegistroDao()
    private val hodometro = HodometroRepositorio(FakeLeituraHodometroDao(), transacao, relogio, AgendadorFalso())
    private val plano = PlanoRepositorio(itensDao, transacao, relogio, AgendadorFalso())

    private fun viewModel(preferencias: FakePreferencias) = AtalhoTrocaDeOleoViewModel(
        SavedStateHandle(mapOf(MotoDetalheViewModel.ARG_MOTO_ID to "m")),
        preferencias,
        plano,
        AtalhoTrocaDeOleo(
            plano,
            RegistroRepositorio(registrosDao, FakeFotoRegistroDao(), hodometro, transacao, relogio),
            hodometro,
            transacao,
            relogio,
        ),
    )

    @Test
    fun noModoOficinaDoisToquesDentroDaMotoGravamATroca() {
        val vm = viewModel(FakePreferencias(AjusteTema(modoOficina = true)))
        rule.setContent { DiarioTheme(modoOficina = true) { AtalhoTrocaDeOleoRoute(vm) } }

        rule.onNodeWithText("Registrar troca de óleo").performClick()
        rule.onNodeWithText("Registrar").performClick()
        rule.waitForIdle()

        assertEquals(listOf("Troca de óleo"), registrosDao.linhas.value.map { it.titulo })
        rule.onNodeWithText("Troca de óleo registrada.").assertExists()
    }

    @Test
    fun foraDoModoOficinaOAtalhoNaoAparece() {
        val vm = viewModel(FakePreferencias(AjusteTema(modoOficina = false)))
        rule.setContent { DiarioTheme { AtalhoTrocaDeOleoRoute(vm) } }
        rule.onNodeWithText("Registrar troca de óleo").assertDoesNotExist()
    }
}
