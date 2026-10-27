package com.pedrohrsm.diariodebordo.plano.ui

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
import com.github.takahirom.roborazzi.captureRoboImage
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.OrigemItem
import com.pedrohrsm.diariodebordo.plano.dominio.CHAVE_TROCA_DE_OLEO
import com.pedrohrsm.diariodebordo.plano.dominio.CampoItem
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Portão visual do plano (PLANEJAMENTO §1): as telas do plano de manutenção em 360 dp, nos 3 temas, com e sem
 * modo oficina, fonte em 140% e 200%, e teclado aberto no formulário. Os estados seguem os de usuário do plano:
 * `plano` rola até o item destacado pela notificação (vencido, próximo e em dia); `plano-topo` mostra o topo, com o
 * aviso de permissão e as pendências de ocorrência (em fonte 200% o "Resolvida" cai abaixo da dobra); sem moto e
 * com erro ao carregar.
 * Gravar: `./gradlew recordRoborazziDebug`; conferir: `./gradlew verifyRoborazziDebug`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class CapturasPlanoTest(private val caso: Caso) {
    enum class Tela(val pasta: String, val teclado: Boolean = false) {
        PLANO("plano"),
        PLANO_TOPO("plano-topo"),
        PLANO_SEM_MOTO("plano-sem-moto"),
        PLANO_ERRO("plano-erro"),
        ITEM_FORM("item-plano-form", teclado = true),
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
            Tela.PLANO -> PlanoScreen(montarPlano(estradeira, itens, kmAtual = 187_450, relogio), destaque = oleo.id)
            // Sem destaque a lista fica no topo: aviso de permissão, pendências com "Resolvida" e o primeiro item.
            Tela.PLANO_TOPO -> PlanoScreen(montarPlano(estradeira, itens, kmAtual = 187_450, relogio))
            Tela.PLANO_SEM_MOTO -> PlanoScreen(PlanoEstado.SemMoto)
            Tela.PLANO_ERRO -> PlanoScreen(PlanoEstado.Erro("database disk image is malformed"))
            Tela.ITEM_FORM -> ItemPlanoFormScreen(
                ItemPlanoFormEstado(
                    carregando = false,
                    motoId = estradeira.id,
                    nome = "",
                    km = "5.0x0",
                    dias = "",
                    invalidos = setOf(CampoItem.NOME, CampoItem.KM),
                ),
            )
        }
    }

    companion object {
        private const val ALTURA_TECLADO_DP = 300

        private val fuso = ZoneId.of("America/Sao_Paulo")
        private val relogio = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), fuso)

        private fun dia(texto: String) = Instant.parse("${texto}T12:00:00Z").toEpochMilli()

        private val estradeira = Moto(
            id = "1",
            apelido = "Estradeira",
            marca = "Honda",
            modelo = "CB 500X",
            ano = 2021,
            fusoHorario = "America/Sao_Paulo",
        )

        // Passou 450 km do óleo: vencido por km.
        private val oleo = item("Troca de óleo", km = 5_000, dias = 180, ultimaKm = 182_000, ultimaEm = "2026-05-01")
            .copy(chave = CHAVE_TROCA_DE_OLEO)

        private val itens = listOf(
            oleo,
            // Faltam 250 km: vence logo.
            item("Pneus (inspeção)", km = 3_000, dias = 90, ultimaKm = 184_700, ultimaEm = "2026-09-01"),
            item("Filtro de ar", km = 10_000, dias = 365, ultimaKm = 180_000, ultimaEm = "2026-03-01"),
            // Só por tempo, passou há 49 dias: vencido por data.
            item("Fluido de freio", km = null, dias = 730, ultimaKm = 150_000, ultimaEm = "2024-09-01"),
            ItemDoPlano(
                motoId = "1",
                nome = "Conferir folga e lubrificação da relação",
                origem = OrigemItem.OCORRENCIA,
            ),
            ItemDoPlano(motoId = "1", nome = "Conferir escapamento e fixações", origem = OrigemItem.OCORRENCIA),
        )

        private fun item(nome: String, km: Int?, dias: Int?, ultimaKm: Int, ultimaEm: String) = ItemDoPlano(
            motoId = "1",
            nome = nome,
            intervaloKm = km,
            intervaloDias = dias,
            ultimaVezKm = ultimaKm,
            ultimaVezEm = dia(ultimaEm),
            origem = OrigemItem.PLANO_BASE,
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
