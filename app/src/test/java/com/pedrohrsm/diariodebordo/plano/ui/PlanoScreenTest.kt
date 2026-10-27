package com.pedrohrsm.diariodebordo.plano.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.OrigemItem
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Os estados da tela do plano: um erro do banco aparece como erro, nunca como plano vazio. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlanoScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val moto = Moto(id = "1", apelido = "Bandida", marca = "Honda", modelo = "CB 500")
    private val relogio = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val oleo = ItemDoPlano(
        motoId = "1",
        nome = "Troca de óleo",
        intervaloKm = 5_000,
        ultimaVezKm = 10_000,
        ultimaVezEm = Instant.parse("2026-10-01T12:00:00Z").toEpochMilli(),
    )
    private val pendencia = ItemDoPlano(motoId = "1", nome = "Conferir manetes", origem = OrigemItem.OCORRENCIA)

    private fun texto(id: Int, vararg args: Any) = RuntimeEnvironment.getApplication().getString(id, *args)

    private fun mostrar(estado: PlanoEstado, acoes: AcoesPlano = AcoesPlano()) {
        rule.setContent { DiarioTheme { PlanoScreen(estado, acoes) } }
    }

    private fun pronto(km: Int) = montarPlano(moto, listOf(oleo, pendencia), km, relogio)

    @Test
    fun falhaDoBancoMostraErroENaoPlanoVazio() {
        mostrar(PlanoEstado.Erro("disco cheio"))
        rule.onNodeWithText(texto(R.string.erro_ao_carregar), substring = true).assertIsDisplayed()
        rule.onNodeWithText(texto(R.string.item_plano_novo)).assertDoesNotExist()
    }

    @Test
    fun semMotoLevaAoCadastro() {
        var cadastrou = false
        mostrar(PlanoEstado.SemMoto, AcoesPlano(onCadastrarMoto = { cadastrou = true }))
        rule.onNodeWithText(texto(R.string.garagem_cadastrar)).performClick()
        assertEquals(true, cadastrou)
    }

    @Test
    fun itemVencidoApareceComoVencidoComQuantoPassou() {
        mostrar(pronto(km = 15_450))
        rule.onNodeWithText(texto(R.string.plano_vencido)).assertIsDisplayed()
        rule.onNodeWithText(texto(R.string.plano_passou, "450 km"), substring = true).assertIsDisplayed()
    }

    @Test
    fun itemQueVenceLogoEItemEmDiaTemSeuProprioRotulo() {
        mostrar(pronto(km = 14_700))
        rule.onNodeWithText(texto(R.string.plano_proximo)).assertIsDisplayed()
        rule.onNodeWithText(texto(R.string.plano_faltam, "300 km"), substring = true).assertIsDisplayed()
    }

    @Test
    fun fizAgoraEResolvidaChamamAsAcoesDoItemCerto() {
        var feito: ItemDoPlano? = null
        var resolvida: String? = null
        mostrar(
            pronto(km = 12_000),
            AcoesPlano(onMarcarFeito = { feito = it }, onResolver = { resolvida = it }),
        )
        rule.onNodeWithText(texto(R.string.plano_resolver)).performClick()
        assertEquals(pendencia.id, resolvida)
        assertEquals(null, feito)
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(texto(R.string.plano_marcar_feito)))
        rule.onNodeWithText(texto(R.string.plano_marcar_feito)).performClick()
        assertEquals(oleo.id, feito?.id)
    }

    @Test
    fun planoMostraAAvisoDeQueOsIntervalosSaoDoUsuarioEOBotaoDeNovoItem() {
        mostrar(pronto(km = 12_000))
        rule.onNodeWithText(texto(R.string.item_plano_novo)).assertIsDisplayed()
        // A lista rola: o aviso fica no fim, depois dos itens.
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(texto(R.string.plano_dado_do_usuario)))
        rule.onNodeWithText(texto(R.string.plano_dado_do_usuario)).assertIsDisplayed()
    }
}
