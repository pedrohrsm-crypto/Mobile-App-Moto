package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.dominio.PontoDoHodometro
import com.pedrohrsm.diariodebordo.garagem.dominio.Vizinha
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Portão visual da correção do hodômetro (#66): o diálogo de edição com a recusa "entre X e Y" e a confirmação
 * de apagar, em 360 dp, nos 3 temas, com e sem modo oficina, fonte em 140% e 200%.
 * A janela do diálogo é capturada à parte; o teclado em si não é desenhado pela JVM.
 * Gravar: `./gradlew recordRoborazziDebug`; conferir: `./gradlew verifyRoborazziDebug`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class CapturasHodometroTest(private val caso: Caso) {
    enum class Dialogo(val pasta: String) {
        CORRIGIR_COM_RECUSA("hodometro-corrigir"),
        APAGAR("hodometro-apagar"),
    }

    data class Caso(val dialogo: Dialogo, val tema: TemaDiario, val modoOficina: Boolean, val fonte: Float) {
        val arquivo: String
            get() = "src/test/snapshots/hodometro/${dialogo.pasta}/${tema.name.lowercase()}" +
                "-${if (modoOficina) "oficina" else "normal"}-fonte${(fonte * 100).toInt()}.png"

        override fun toString() = arquivo.removePrefix("src/test/snapshots/").removeSuffix(".png")
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun captura() {
        rule.setContent {
            val densidade = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(densidade.density, caso.fonte)) {
                DiarioTheme(tema = caso.tema, modoOficina = caso.modoOficina) {
                    // A janela do AlertDialog não fica ociosa na JVM: captura-se o corpo do diálogo numa superfície.
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.width(312.dp)) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            when (caso.dialogo) {
                                Dialogo.CORRIGIR_COM_RECUSA -> {
                                    val dialogo = DialogoHodometro(
                                        texto = "187.900",
                                        erro = ErroHodometro.ForaDoIntervalo(Vizinha.ANTERIOR, anterior, seguinte),
                                        editando = LeituraHodometro(
                                            motoId = "1",
                                            km = 186_900,
                                            lidaEm = anterior.lidaEm,
                                        ),
                                    )
                                    Text(stringResource(tituloDoDialogo(dialogo)))
                                    CampoDialogoHodometro(dialogo, onTexto = {})
                                }
                                Dialogo.APAGAR -> {
                                    val confirmacao = ConfirmacaoApagar(
                                        leitura = LeituraHodometro(
                                            motoId = "1",
                                            km = 186_900,
                                            lidaEm = seguinte.lidaEm,
                                            fusoHorario = "America/Sao_Paulo",
                                        ),
                                        erro = ErroApagar.UnicaComRegistrosDeKm,
                                    )
                                    Text(stringResource(R.string.hodometro_apagar_titulo, "186.900 km"))
                                    TextoDialogoApagar(confirmacao)
                                }
                            }
                        }
                    }
                }
            }
        }
        rule.onRoot().captureRoboImage(caso.arquivo)
    }

    companion object {
        private fun dia(d: Int) = Instant.parse("2026-10-${"%02d".format(d)}T12:00:00Z").toEpochMilli()

        private val anterior = PontoDoHodometro("a", 180_120, dia(10), "America/Sao_Paulo")
        private val seguinte = PontoDoHodometro("c", 187_450, dia(20), "America/Sao_Paulo")

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun casos(): List<Array<Any>> = buildList {
            for (dialogo in Dialogo.entries) {
                for (tema in TemaDiario.entries) {
                    for (modoOficina in listOf(false, true)) {
                        for (fonte in listOf(1.4f, 2.0f)) add(arrayOf(Caso(dialogo, tema, modoOficina, fonte)))
                    }
                }
            }
        }
    }
}
