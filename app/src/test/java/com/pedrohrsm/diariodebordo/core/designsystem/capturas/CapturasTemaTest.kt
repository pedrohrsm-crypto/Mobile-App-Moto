package com.pedrohrsm.diariodebordo.core.designsystem.capturas

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.FormularioDiario
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.SeletorTema
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.TelaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.ui.GaragemEstado
import com.pedrohrsm.diariodebordo.garagem.ui.GaragemScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * L3 da frente fundacao-tema: a pior condição do plano (360 dp, fonte em 140% e 200%) nos 3 temas, com e sem
 * modo oficina, em 3 telas. Gravar: `./gradlew recordRoborazziDebug`; conferir: `./gradlew verifyRoborazziDebug`.
 * Sem esses comandos o teste só compõe as telas (fumaça) e não compara pixel.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h900dp-xhdpi")
class CapturasTemaTest(private val caso: Caso) {
    enum class Tela(val pasta: String) { GARAGEM("garagem"), AJUSTE_TEMA("ajuste-tema"), PALETA("paleta") }

    data class Caso(val tela: Tela, val tema: TemaDiario, val modoOficina: Boolean, val fonte: Float) {
        val arquivo: String
            get() = "src/test/snapshots/${tela.pasta}/${tema.name.lowercase()}" +
                "-${if (modoOficina) "oficina" else "normal"}-fonte${(fonte * 100).toInt()}.png"

        override fun toString() = arquivo.removePrefix("src/test/snapshots/").removeSuffix(".png")
    }

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun captura() {
        rule.setContent {
            // Fonte do sistema simulada pela densidade: o sp cresce como no Android com a fonte em 140% ou 200%.
            val densidade = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(densidade.density, caso.fonte)) {
                DiarioTheme(tema = caso.tema, modoOficina = caso.modoOficina) {
                    when (caso.tela) {
                        Tela.GARAGEM -> GaragemScreen(GaragemEstado.Pronto(motosDeExemplo))
                        Tela.AJUSTE_TEMA -> TelaDiario(stringResource(R.string.ajuste_tema_titulo)) { padding ->
                            FormularioDiario(padding) {
                                SeletorTema(AjusteTema(caso.tema, caso.modoOficina), aoAjustar = {})
                            }
                        }
                        Tela.PALETA -> PaletaDeTokens()
                    }
                }
            }
        }
        rule.onRoot().captureRoboImage(caso.arquivo)
    }

    companion object {
        private val motosDeExemplo = listOf(
            Moto(id = "1", apelido = "Estradeira", marca = "Honda", modelo = "CB 500X", ano = 2021),
            Moto(id = "2", apelido = "Cidade", marca = "Yamaha", modelo = "Fazer 250", ano = 2018),
            Moto(id = "3", apelido = "Trilha", marca = "Honda", modelo = "XRE 300", ano = 2015),
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
