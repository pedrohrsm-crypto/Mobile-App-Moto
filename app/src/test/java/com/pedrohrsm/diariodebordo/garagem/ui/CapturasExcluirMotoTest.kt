package com.pedrohrsm.diariodebordo.garagem.ui

import android.os.Looper
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.dominio.ContagemDaMoto
import java.time.Duration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Portão visual da exclusão da moto (#65): o `AlertDialog` real, com a contagem, o atalho do backup e o apelido já
 * digitado (botão habilitado), sobre a tela da moto e com o teclado aberto (a pessoa está digitando), em 360 dp, nos
 * 3 temas, com e sem modo oficina, fonte em 140% e 200%. O diálogo abre noutra janela: a captura da tela inteira
 * pega as duas. Gravar: `./gradlew recordRoborazziDebug`; conferir: `./gradlew verifyRoborazziDebug`.
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
        // O campo do diálogo não deixa o Compose ficar ocioso (AppNotIdle no setContent da regra, como no #66):
        // monta-se a tela direto na activity. Diferente do #66, o texto do diálogo rola: com o relógio parado o
        // looper nunca esvazia (idle() não volta), então o relógio anda sozinho e o looper roda um tempo fixo.
        rule.activity.setContent {
            DiarioTheme(tema = caso.tema, modoOficina = caso.modoOficina) {
                Box(Modifier.fillMaxSize()) {
                    MotoDetalheScreen(MotoDetalheEstado.Pronto(moto, leituras, kmAtual = 23_450))
                    DialogoExcluirMoto(
                        dialogo = DialogoExclusao(apelido = moto.apelido, contagem = contagem, texto = moto.apelido),
                        onTexto = {},
                        onExcluir = {},
                        onBackup = {},
                        onFechar = {},
                    )
                    // O teclado em si não é desenhado pela JVM: esta faixa ocupa o lugar dele na captura.
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .windowInsetsBottomHeight(WindowInsets.ime)
                            .background(MaterialTheme.colorScheme.inverseSurface),
                    )
                }
            }
        }
        rodar()
        abrirTeclado()
        rodar()
        captureScreenRoboImage(caso.arquivo)
    }

    /** Teclado aberto: o sistema manda um inset de IME de [ALTURA_TECLADO_DP] para a janela, como no aparelho. */
    private fun abrirTeclado() {
        rule.runOnUiThread {
            val raiz = rule.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
            val altura = (ALTURA_TECLADO_DP * raiz.resources.displayMetrics.density).toInt()
            val ime = WindowInsetsCompat.Type.ime()
            val insets = WindowInsetsCompat.Builder()
                .setInsets(ime, Insets.of(0, 0, 0, altura))
                .setVisible(ime, true)
                .build()
            ViewCompat.dispatchApplyWindowInsets(raiz, insets)
        }
    }

    private fun rodar() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ESPERA_MS))

    companion object {
        private const val ALTURA_TECLADO_DP = 300
        private const val ESPERA_MS = 1_000L
        private val moto = Moto(id = "1", apelido = "Bandida", marca = "Harley-Davidson", modelo = "Sportster 883")
        private val contagem = ContagemDaMoto(registros = 12, fotos = 5, itensDoPlano = 7, rotas = 3, leituras = 4)
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
