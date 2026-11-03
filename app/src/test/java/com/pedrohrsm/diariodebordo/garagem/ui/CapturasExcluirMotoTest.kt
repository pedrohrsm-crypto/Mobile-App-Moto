package com.pedrohrsm.diariodebordo.garagem.ui

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.dominio.ContagemDaMoto
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import java.time.Duration
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Portão visual da exclusão da moto (#65): o diálogo sobre a tela da moto, com o apelido já digitado (botão
 * habilitado), em 360 dp, nos 3 temas, com e sem modo oficina, fonte em 140% e 200%.
 * Gravar: `./gradlew recordRoborazziDebug`; conferir: `./gradlew verifyRoborazziDebug`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class CapturasExcluirMotoTest(private val caso: Caso) {

    data class Caso(val tema: TemaDiario, val modoOficina: Boolean, val fonte: Float) {
        val arquivo: String
            get() = "src/test/snapshots/garagem/excluir-moto/${tema.name.lowercase()}" +
                "-${if (modoOficina) "oficina" else "normal"}-fonte${(fonte * 100).toInt()}.png"

        override fun toString() = arquivo.removePrefix("src/test/snapshots/").removeSuffix(".png")
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun captura() {
        // O diálogo abre noutra janela, com a densidade dela: a fonte do sistema vale para todas as janelas.
        RuntimeEnvironment.setFontScale(caso.fonte)
        // O campo de texto dentro do diálogo nunca deixa o Compose ocioso no Robolectric (o mesmo vale para o
        // diálogo do hodômetro, que por isso não tinha captura): o conteúdo entra direto na activity, sem a espera
        // de ociosidade da regra, e o looper anda um tempo fixo antes da captura.
        rule.activity.setContent {
            DiarioTheme(tema = caso.tema, modoOficina = caso.modoOficina) {
                MotoDetalheScreen(MotoDetalheEstado.Pronto(moto, leituras, kmAtual = 23_450))
                DialogoExcluirMoto(
                    dialogo = DialogoExclusao(
                        apelido = moto.apelido,
                        contagem = ContagemDaMoto(
                            registros = 12,
                            fotos = 5,
                            itensDoPlano = 7,
                            rotas = 3,
                            leituras = 4,
                        ),
                        texto = moto.apelido,
                    ),
                    onTexto = {},
                    onExcluir = {},
                    onBackup = {},
                    onFechar = {},
                )
            }
        }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ABERTURA_MS))
        captureScreenRoboImage(caso.arquivo)
    }

    companion object {
        private const val ABERTURA_MS = 2_000L
        private val moto = Moto(id = "1", apelido = "Bandida", marca = "Harley-Davidson", modelo = "Sportster 883")
        private val leituras = listOf(
            LeituraHodometro(motoId = "1", km = 23_450, lidaEm = 1_790_000_000_000, fusoHorario = "America/Sao_Paulo"),
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun casos(): List<Array<Any>> = buildList {
            for (tema in TemaDiario.entries) {
                for (modoOficina in listOf(false, true)) {
                    for (fonte in listOf(1.4f, 2.0f)) add(arrayOf(Caso(tema, modoOficina, fonte)))
                }
            }
        }
    }
}
