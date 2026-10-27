package com.pedrohrsm.diariodebordo.core.ui

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.github.takahirom.roborazzi.captureRoboImage
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.TipoOcorrencia
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.diario.dominio.CampoRegistro
import com.pedrohrsm.diariodebordo.diario.ui.DiarioEstado
import com.pedrohrsm.diariodebordo.diario.ui.DiarioScreen
import com.pedrohrsm.diariodebordo.diario.ui.RegistroFormEstado
import com.pedrohrsm.diariodebordo.diario.ui.RegistroFormScreen
import com.pedrohrsm.diariodebordo.garagem.data.CategoriaMoto
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.dominio.CampoMoto
import com.pedrohrsm.diariodebordo.garagem.ui.GaragemEstado
import com.pedrohrsm.diariodebordo.garagem.ui.GaragemScreen
import com.pedrohrsm.diariodebordo.garagem.ui.MotoDetalheEstado
import com.pedrohrsm.diariodebordo.garagem.ui.MotoDetalheScreen
import com.pedrohrsm.diariodebordo.garagem.ui.MotoFormEstado
import com.pedrohrsm.diariodebordo.garagem.ui.MotoFormScreen
import java.time.Instant
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Portão visual da Fase 1 (PLANEJAMENTO §1): as telas de garagem, hodômetro e diário em 360 dp, nos 3 temas,
 * com e sem modo oficina, fonte em 140% e 200%, e teclado aberto nos formulários. Os dados de exemplo seguem
 * os estados de usuário do plano: sem moto, moto nova, hodômetro alto e diário com histórico.
 * Gravar: `./gradlew recordRoborazziDebug`; conferir: `./gradlew verifyRoborazziDebug`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class CapturasFase1Test(private val caso: Caso) {
    enum class Tela(val pasta: String, val teclado: Boolean = false) {
        GARAGEM_COM_KM("garagem-com-km"),
        MOTO_FORM("moto-form", teclado = true),
        MOTO_DETALHE("moto-detalhe"),
        DIARIO("diario"),
        DIARIO_SEM_MOTO("diario-sem-moto"),
        REGISTRO_FORM("registro-form", teclado = true),
    }

    data class Caso(val tela: Tela, val tema: TemaDiario, val modoOficina: Boolean, val fonte: Float) {
        val arquivo: String
            get() = "src/test/snapshots/fase1/${tela.pasta}/${tema.name.lowercase()}" +
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
                DiarioTheme(tema = caso.tema, modoOficina = caso.modoOficina) {
                    Box(Modifier.fillMaxSize()) {
                        Tela(caso.tela)
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
        if (caso.tela.teclado) abrirTeclado()
        rule.onRoot().captureRoboImage(caso.arquivo)
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
        rule.waitForIdle()
    }

    @Composable
    private fun Tela(tela: Tela) {
        when (tela) {
            Tela.GARAGEM_COM_KM -> GaragemScreen(
                GaragemEstado.Pronto(listOf(estradeira), mapOf(estradeira.id to 187_450), podeAdicionar = false),
            )
            Tela.MOTO_FORM -> MotoFormScreen(
                MotoFormEstado(
                    apelido = "Estradeira",
                    marca = "Honda",
                    modelo = "",
                    ano = "2021",
                    kmInicial = "12,5",
                    categoria = CategoriaMoto.TRAIL,
                    invalidos = setOf(CampoMoto.MODELO, CampoMoto.KM),
                ),
            )
            Tela.MOTO_DETALHE -> MotoDetalheScreen(MotoDetalheEstado.Pronto(estradeira, leituras))
            Tela.DIARIO -> DiarioScreen(
                DiarioEstado.Pronto(estradeira, filtro = null),
                flowOf(PagingData.from(historico)).collectAsLazyPagingItems(),
            )
            Tela.DIARIO_SEM_MOTO -> DiarioScreen(
                DiarioEstado.SemMoto,
                flowOf(PagingData.empty<Registro>()).collectAsLazyPagingItems(),
            )
            Tela.REGISTRO_FORM -> RegistroFormScreen(
                RegistroFormEstado(
                    carregando = false,
                    motoId = estradeira.id,
                    tipo = TipoRegistro.ABASTECIMENTO,
                    titulo = "Posto da serra",
                    dataEm = dia(1),
                    km = "187.450",
                    valor = "89,999",
                    litros = "12,5",
                    tanqueCheio = true,
                    invalidos = setOf(CampoRegistro.VALOR),
                ),
            )
        }
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

        private val leituras = listOf(187_450, 186_900, 180_120, 12_000).mapIndexed { i, km ->
            LeituraHodometro(motoId = "1", km = km, lidaEm = dia(20 - i * 5), fusoHorario = "America/Sao_Paulo")
        }

        private val historico = listOf(
            Registro(
                motoId = "1",
                tipo = TipoRegistro.ABASTECIMENTO,
                titulo = "Posto da serra",
                dataEm = dia(20),
                hodometroKm = 187_450,
                valorCentavos = 8_990,
                litrosMl = 12_500,
                fusoHorario = "America/Sao_Paulo",
            ),
            Registro(
                motoId = "1",
                tipo = TipoRegistro.MANUTENCAO,
                titulo = "Troca de óleo e filtro",
                dataEm = dia(15),
                hodometroKm = 186_900,
                valorCentavos = 34_500,
                fusoHorario = "America/Sao_Paulo",
            ),
            Registro(
                motoId = "1",
                tipo = TipoRegistro.OCORRENCIA,
                titulo = "Barulho na corrente",
                dataEm = dia(10),
                ocorrencia = TipoOcorrencia.BARULHO,
                fusoHorario = "America/Sao_Paulo",
            ),
            Registro(
                motoId = "1",
                tipo = TipoRegistro.ROLE,
                titulo = "Serra do Rio do Rastro",
                dataEm = dia(5),
                fusoHorario = "America/Sao_Paulo",
            ),
            Registro(
                motoId = "1",
                tipo = TipoRegistro.NOTA,
                titulo = "Pneu traseiro com 60% de vida",
                dataEm = dia(1),
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
