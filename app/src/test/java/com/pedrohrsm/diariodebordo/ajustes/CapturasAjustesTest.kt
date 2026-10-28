package com.pedrohrsm.diariodebordo.ajustes

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pedrohrsm.diariodebordo.backup.ui.AvisoDadosPessoais
import com.pedrohrsm.diariodebordo.backup.ui.BackupEstado
import com.pedrohrsm.diariodebordo.backup.ui.EscolhaDaMoto
import com.pedrohrsm.diariodebordo.backup.ui.SecaoBackupConteudo
import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.ui.AcoesMoto
import com.pedrohrsm.diariodebordo.garagem.ui.MotoDetalheEstado
import com.pedrohrsm.diariodebordo.garagem.ui.MotoDetalheScreen
import com.pedrohrsm.diariodebordo.plano.ui.AcaoAtalhoOleo
import com.pedrohrsm.diariodebordo.plano.ui.AtalhoTrocaDeOleoView
import com.pedrohrsm.diariodebordo.plano.ui.EstadoAtalhoOleo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Portão visual da frente de ajustes e backup (#45): a tela de ajustes, o aviso de dados pessoais da exportação,
 * a escolha da moto na importação (decisão #13) e a moto com o atalho da troca de óleo, em 360 dp, nos 3 temas,
 * com e sem modo oficina, fonte em 140% e 200%. Captura do estado de abertura, sem rolar.
 * O atalho só existe no modo oficina: a tela da moto com ele só é capturada nesse modo.
 * Gravar: `./gradlew recordRoborazziDebug`; conferir: `./gradlew verifyRoborazziDebug`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class CapturasAjustesTest(private val caso: Caso) {
    enum class Tela(val pasta: String, val dialogo: Boolean = false, val soOficina: Boolean = false) {
        AJUSTES("ajustes"),
        BACKUP("backup"),
        AVISO_EXPORTAR("backup-aviso", dialogo = true),
        ESCOLHA_DA_MOTO("backup-escolha-moto", dialogo = true),
        MOTO_ATALHO_OLEO("moto-atalho-oleo", soOficina = true),
        CONFIRMAR_OLEO("moto-atalho-oleo-confirmar", dialogo = true, soOficina = true),
    }

    data class Caso(val tela: Tela, val tema: TemaDiario, val modoOficina: Boolean, val fonte: Float) {
        val arquivo: String
            get() = "src/test/snapshots/ajustes/${tela.pasta}/${tema.name.lowercase()}" +
                "-${if (modoOficina) "oficina" else "normal"}-fonte${(fonte * 100).toInt()}.png"

        override fun toString() = arquivo.removePrefix("src/test/snapshots/").removeSuffix(".png")
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun captura() {
        // O diálogo abre noutra janela, com a densidade dela: a fonte do sistema vale para todas as janelas.
        if (caso.tela.dialogo) RuntimeEnvironment.setFontScale(caso.fonte)
        rule.setContent {
            val densidade = LocalDensity.current
            val fonte = if (caso.tela.dialogo) densidade.fontScale else caso.fonte
            CompositionLocalProvider(LocalDensity provides Density(densidade.density, fonte)) {
                DiarioTheme(tema = caso.tema, modoOficina = caso.modoOficina) { Tela(caso.tela) }
            }
        }
        // O diálogo abre noutra janela: a captura da tela inteira pega as duas.
        if (caso.tela.dialogo) captureScreenRoboImage(caso.arquivo) else rule.onRoot().captureRoboImage(caso.arquivo)
    }

    @Composable
    private fun Tela(tela: Tela) {
        val ajuste = AjusteTema(caso.tema, caso.modoOficina)
        when (tela) {
            Tela.AJUSTES -> AjustesScreen(ajuste) { SecaoBackupConteudo(BackupEstado()) }
            // A seção de backup fica abaixo da dobra nos ajustes: capturada sozinha, no estado "trabalhando".
            Tela.BACKUP -> Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(LocalEscalaDiario.current.espaco(16.dp))) {
                    SecaoBackupConteudo(BackupEstado(ocupado = true))
                }
            }
            Tela.AVISO_EXPORTAR -> {
                AjustesScreen(ajuste) { SecaoBackupConteudo(BackupEstado()) }
                AvisoDadosPessoais(comRotas = false)
            }
            Tela.ESCOLHA_DA_MOTO -> {
                AjustesScreen(ajuste) { SecaoBackupConteudo(BackupEstado(ocupado = true)) }
                EscolhaDaMoto(listOf(estradeira, bandida))
            }
            Tela.MOTO_ATALHO_OLEO -> Moto(AcaoAtalhoOleo(registrado = true))
            Tela.CONFIRMAR_OLEO -> Moto(AcaoAtalhoOleo(confirmando = true))
        }
    }

    @Composable
    private fun Moto(acao: AcaoAtalhoOleo) {
        MotoDetalheScreen(
            MotoDetalheEstado.Pronto(estradeira, leituras, kmAtual = 187_450),
            AcoesMoto(atalhos = { AtalhoTrocaDeOleoView(EstadoAtalhoOleo.Disponivel, acao) }),
        )
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
        private val bandida = Moto(id = "2", apelido = "Bandida", marca = "Harley-Davidson", modelo = "Sportster 883")
        private val leituras = listOf(
            LeituraHodometro(motoId = "1", km = 187_450, lidaEm = 1_790_000_000_000, fusoHorario = "America/Sao_Paulo"),
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun casos(): List<Array<Any>> = buildList {
            for (tela in Tela.entries) {
                for (tema in TemaDiario.entries) {
                    for (modoOficina in listOf(false, true)) {
                        if (tela.soOficina && !modoOficina) continue
                        for (fonte in listOf(1.4f, 2.0f)) add(arrayOf(Caso(tela, tema, modoOficina, fonte)))
                    }
                }
            }
        }
    }
}
