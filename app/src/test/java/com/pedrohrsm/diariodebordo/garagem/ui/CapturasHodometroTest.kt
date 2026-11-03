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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.dominio.PontoDoHodometro
import com.pedrohrsm.diariodebordo.garagem.dominio.Vizinha
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Portão visual da correção do hodômetro (#66): o `AlertDialog` real de edição, com a recusa "entre X e Y" e o
 * teclado aberto, e o de confirmação de apagar, sobre a tela da moto, em 360 dp, nos 3 temas, com e sem modo
 * oficina, fonte em 140% e 200%. O diálogo abre noutra janela: a captura da tela inteira pega as duas.
 * Gravar: `./gradlew recordRoborazziDebug`; conferir: `./gradlew verifyRoborazziDebug`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class CapturasHodometroTest(private val caso: Caso) {
    enum class Dialogo(val pasta: String, val teclado: Boolean = false) {
        CORRIGIR_COM_RECUSA("hodometro-corrigir", teclado = true),
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

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun captura() {
        // O diálogo abre noutra janela, com a densidade dela: a fonte do sistema vale para todas as janelas.
        RuntimeEnvironment.setFontScale(caso.fonte)
        // O campo do diálogo de edição não deixa o Compose ficar ocioso (AppNotIdle no setContent da regra):
        // monta-se a tela direto na activity e avança-se o looper à mão.
        rule.mainClock.autoAdvance = false
        rule.activity.setContent {
            val densidade = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(densidade.density, densidade.fontScale)) {
                DiarioTheme(tema = caso.tema, modoOficina = caso.modoOficina) {
                    Box(Modifier.fillMaxSize()) {
                        MotoDetalheScreen(MotoDetalheEstado.Pronto(estradeira, leituras, kmAtual = 187_450))
                        when (caso.dialogo) {
                            Dialogo.CORRIGIR_COM_RECUSA -> DialogoHodometroView(
                                dialogo = DialogoHodometro(
                                    texto = "187.900",
                                    erro = ErroHodometro.ForaDoIntervalo(Vizinha.SEGUINTE, anterior, seguinte),
                                    editando = leituras[1],
                                ),
                                onTexto = {},
                                onSalvar = {},
                                onFechar = {},
                            )
                            Dialogo.APAGAR -> DialogoApagarLeituraView(
                                confirmacao = ConfirmacaoApagar(leituras[1]),
                                onConfirmar = {},
                                onFechar = {},
                            )
                        }
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
        }
        shadowOf(Looper.getMainLooper()).idle()
        if (caso.dialogo.teclado) abrirTeclado()
        rule.mainClock.advanceTimeByFrame()
        shadowOf(Looper.getMainLooper()).idle()
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
        shadowOf(Looper.getMainLooper()).idle()
    }

    companion object {
        private const val ALTURA_TECLADO_DP = 300

        private val estradeira = Moto(
            id = "1",
            apelido = "Estradeira",
            marca = "Honda",
            modelo = "CB 500X",
            ano = 2021,
            fusoHorario = "America/Sao_Paulo",
        )

        private fun dia(d: Int) = Instant.parse("2026-10-${"%02d".format(d)}T12:00:00Z").toEpochMilli()

        /** Da mais nova para a mais antiga, como a lista da tela; a do meio (186.900) é a que se edita ou apaga. */
        private val leituras = listOf(187_450, 186_900, 180_120).mapIndexed { i, km ->
            LeituraHodometro(motoId = "1", km = km, lidaEm = dia(20 - i * 5), fusoHorario = "America/Sao_Paulo")
        }

        /** As vizinhas pela data da leitura do meio: 180.120 (dia 10) e 187.450 (dia 20). */
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
