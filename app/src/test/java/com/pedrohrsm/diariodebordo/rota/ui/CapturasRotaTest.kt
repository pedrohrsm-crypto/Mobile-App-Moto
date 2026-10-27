package com.pedrohrsm.diariodebordo.rota.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.TelaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.diario.ui.AcoesRegistroForm
import com.pedrohrsm.diariodebordo.diario.ui.CampoLocal
import com.pedrohrsm.diariodebordo.diario.ui.RegistroFormEstado
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.localizacao.Ponto
import com.pedrohrsm.diariodebordo.rota.data.Rota
import com.pedrohrsm.diariodebordo.rota.dominio.Polilinha
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Portão visual do GPS: o campo de local do registro (1.6) e as rotas (1.7) com gravação em curso, gravação
 * interrompida e o traçado salvo, em 360 dp, * nos 3 temas, com e sem modo oficina, fonte em 140% e 200%. Nenhuma delas tem campo de texto: sem teclado.
 * Gravar: `./gradlew recordRoborazziDebug`; conferir: `./gradlew verifyRoborazziDebug`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class CapturasRotaTest(private val caso: Caso) {
    enum class Tela(val pasta: String) {
        LOCAL_REGISTRO("local-registro"),
        ROTAS_OCIOSA("rotas-ociosa"),
        ROTAS_GRAVANDO("rotas-gravando"),
        ROTAS_INTERROMPIDA("rotas-interrompida"),
        ROTA_DETALHE("rota-detalhe"),
    }

    data class Caso(val tela: Tela, val tema: TemaDiario, val modoOficina: Boolean, val fonte: Float) {
        val arquivo: String
            get() = "src/test/snapshots/rota/${tela.pasta}/${tema.name.lowercase()}" +
                "-${if (modoOficina) "oficina" else "normal"}-fonte${(fonte * 100).toInt()}.png"

        override fun toString() = arquivo.removePrefix("src/test/snapshots/").removeSuffix(".png")
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun captura() {
        rule.setContent {
            // Fonte do sistema simulada pela densidade: o sp cresce como no Android com a fonte em 140% ou 200%.
            val densidade = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(densidade.density, caso.fonte)) {
                DiarioTheme(tema = caso.tema, modoOficina = caso.modoOficina) { Tela(caso.tela) }
            }
        }
        rule.onRoot().captureRoboImage(caso.arquivo)
    }

    @Composable
    private fun Tela(tela: Tela) {
        when (tela) {
            Tela.LOCAL_REGISTRO -> TelaDiario(titulo = stringResource(R.string.registro_novo)) { padding ->
                // Com local marcado, buscando o GPS e sem local: os três jeitos do campo no formulário.
                Column(Modifier.padding(padding).padding(LocalEscalaDiario.current.espaco(16.dp))) {
                    CampoLocal(RegistroFormEstado(latitude = -28.0123, longitude = -49.5987), AcoesRegistroForm())
                    CampoLocal(RegistroFormEstado(buscandoLocal = true), AcoesRegistroForm())
                    CampoLocal(RegistroFormEstado(), AcoesRegistroForm())
                }
            }
            Tela.ROTAS_OCIOSA -> RotasScreen(RotasEstado.Pronto(estradeira, gravacao = null, rotas = emptyList()))
            Tela.ROTAS_GRAVANDO -> RotasScreen(
                RotasEstado.Pronto(estradeira, Gravacao(emGravacao, pontos = 1_284, capturando = true), salvas),
            )
            Tela.ROTAS_INTERROMPIDA -> RotasScreen(
                RotasEstado.Pronto(estradeira, Gravacao(emGravacao, pontos = 312, capturando = false), salvas),
            )
            Tela.ROTA_DETALHE -> RotaDetalheTela(
                RotaDetalheEstado.Pronto(salvas.first(), Polilinha.decodificar(tracado)),
            )
        }
    }

    companion object {
        private val estradeira = Moto(
            id = "1",
            apelido = "Estradeira",
            marca = "Honda",
            modelo = "CB 500X",
            ano = 2021,
            fusoHorario = "America/Sao_Paulo",
        )

        private fun hora(h: Int) = Instant.parse("2026-10-04T${"%02d".format(h)}:00:00Z").toEpochMilli()

        // Traçado inventado (uma serra em zigue-zague), longe de qualquer endereço real.
        private val tracado = Polilinha.codificar(
            (0..12).map { i -> Ponto(-28.0 + i * 0.004, -49.6 + (if (i % 2 == 0) 0.0 else 0.006) + i * 0.002, 0) },
        )

        private val emGravacao = Rota(motoId = "1", iniciadaEm = hora(13), fusoHorario = "America/Sao_Paulo")

        private val salvas = listOf(
            Rota(
                motoId = "1",
                iniciadaEm = hora(9),
                distanciaM = 87_400,
                duracaoS = 7_260,
                polilinha = tracado,
                emGravacao = false,
                fusoHorario = "America/Sao_Paulo",
            ),
            Rota(
                motoId = "1",
                iniciadaEm = hora(7) - 86_400_000,
                distanciaM = 640,
                duracaoS = 300,
                emGravacao = false,
                fusoHorario = "America/Sao_Paulo",
            ),
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun casos(): List<Array<Any>> = buildList {
            for (tela in Tela.entries) {
                for (tema in TemaDiario.entries) {
                    for (modoOficina in listOf(false, true)) {
                        for (fonte in listOf(1.4f, 2.0f)) add(arrayOf(Caso(tela, tema, modoOficina, fonte)))
                    }
                }
            }
        }
    }
}
