package com.pedrohrsm.diariodebordo.core.designsystem.theme

import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.coresSwitchDiario
import com.pedrohrsm.diariodebordo.navegacao.coresBarraDestinos
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private typealias Papel = (ColorScheme) -> Color

/**
 * Contraste das cores EFETIVAS dos componentes do Material que o app usa (erro #55, 4ª ocorrência da classe).
 * A lista de pares do [ContrasteTemaTest] é escrita à mão e não vê o que um componente usa por padrão. Aqui o
 * [DiarioTheme] é composto nos 3 temas × modo oficina e as cores saem dos `*Defaults` (ou das funções do app,
 * `coresBarraDestinos` e `coresSwitchDiario`). O teste reprova de três modos:
 *  - par efetivo abaixo do mínimo (4,5:1 texto, 3:1 componente de interface);
 *  - cor efetiva diferente do papel do ColorScheme declarado no par (o padrão do Material mudou);
 *  - componente do Material importado no app sem nenhum par aqui (ou sem exceção nomeada em [SEM_COR_PROPRIA]).
 * Estados desabilitados ficam de fora: a WCAG os isenta.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ContrasteComponentesTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private class ParEfetivo(
        val componente: String,
        val nome: String,
        val frente: Color,
        val fundo: Color,
        val minimo: Double,
        val papelFrente: Papel,
        val papelFundo: Papel,
    ) {
        val razao: Double get() = ContrasteWcag.contraste(frente, fundo)
    }

    private class Configuracao(val tema: TemaDiario, val modoOficina: Boolean, val tokens: TokensDiario? = null)

    private var configuracao by mutableStateOf(Configuracao(TemaDiario.OFICINA, false))
    private var coletados: List<ParEfetivo> = emptyList()
    private var esquemaAtual: ColorScheme? = null

    @Composable
    private fun Coleta() {
        esquemaAtual = MaterialTheme.colorScheme
        coletados = coletarPares()
    }

    private fun compor(nova: Configuracao): List<ParEfetivo> {
        configuracao = nova
        rule.waitForIdle()
        return coletados
    }

    private fun iniciar() {
        rule.setContent {
            val c = configuracao
            if (c.tokens == null) {
                DiarioTheme(tema = c.tema, modoOficina = c.modoOficina) { Coleta() }
            } else {
                MaterialTheme(colorScheme = c.tokens.paraColorScheme()) { Coleta() }
            }
        }
    }

    /** Descrição de tudo que está errado na composição atual; vazia quando todos os pares passam. */
    private fun falhas(pares: List<ParEfetivo>): List<String> {
        val esquema = checkNotNull(esquemaAtual)
        return pares.flatMap { par ->
            buildList {
                val frente = par.papelFrente(esquema)
                val fundo = par.papelFundo(esquema)
                if (par.frente !=
                    frente
                ) {
                    add("${par.nome}: frente efetiva ${hex(par.frente)} difere do papel declarado ${hex(frente)}")
                }
                if (par.fundo !=
                    fundo
                ) {
                    add("${par.nome}: fundo efetivo ${hex(par.fundo)} difere do papel declarado ${hex(fundo)}")
                }
                if (par.razao < par.minimo) add("%s %.2f:1 (mínimo %.1f)".format(par.nome, par.razao, par.minimo))
            }
        }
    }

    private fun hex(cor: Color): String = "#%08X".format(cor.toArgb())

    @Test
    fun todoParEfetivoDeComponenteAtingeOMinimoEmCadaTema() {
        iniciar()
        TemaDiario.entries.forEach { tema ->
            listOf(false, true).forEach { modoOficina ->
                val pares = compor(Configuracao(tema, modoOficina))
                assertTrue("nenhum par coletado", pares.isNotEmpty())
                val abaixo = falhas(pares).filterNot { falha -> EXCECOES.keys.any { falha.startsWith(it) } }
                assertTrue("$tema (modo oficina=$modoOficina) reprovou: $abaixo", abaixo.isEmpty())
            }
        }
    }

    @Test
    fun excecoesNomeadasTemMotivoEContinuamReprovando() {
        // Uma exceção que passou a passar é lixo: sai da lista. Uma sem motivo escrito também não vale.
        iniciar()
        val falhasPorPar = TemaDiario.entries.flatMap { tema ->
            listOf(false, true).flatMap { modoOficina -> falhas(compor(Configuracao(tema, modoOficina))) }
        }
        EXCECOES.forEach { (nome, motivo) ->
            assertTrue("exceção '$nome' sem motivo", motivo.length >= MOTIVO_MINIMO)
            assertTrue("exceção '$nome' já passa: remova da lista", falhasPorPar.any { it.startsWith(nome) })
        }
    }

    @Test
    fun iconeSelecionadoDaBarraSeDistingueDoNaoSelecionadoEmCadaTema() {
        // Com a fonte grande a barra só tem ícones; o indicador (secondaryContainer) fica de 1,02 a 1,34:1 sobre a barra,
        // então o que mostra o destino aberto é a cor do ícone, que não pode ser igual à do não selecionado.
        iniciar()
        TemaDiario.entries.forEach { tema ->
            listOf(false, true).forEach { modoOficina ->
                val pares = compor(Configuracao(tema, modoOficina))
                val selecionado = pares.first { it.nome == "barra: ícone selecionado/barra" }.frente
                val naoSelecionado = pares.first { it.nome == "barra: ícone não selecionado/barra" }.frente
                assertNotEquals("$tema (oficina=$modoOficina) ícones iguais", naoSelecionado, selecionado)
            }
        }
    }

    @Test
    fun parPadraoDeOutroComponentePlantadoAbaixoDoMinimoReprova() {
        // Canário de classe (#55): TEXTO_SECUNDARIO rebaixado quebra, sem lista nenhuma, o rótulo não selecionado da
        // barra, o texto do diálogo, o ListItem e o campo de texto. Ninguém escreveu esses pares: saem dos Defaults.
        iniciar()
        val rebaixado = TemaDiario.ESTRADA.tokens() + (PapelCor.TEXTO_SECUNDARIO to Color(0xFF8A9499))
        val abaixo = falhas(compor(Configuracao(TemaDiario.ESTRADA, false, rebaixado)))
        listOf(
            "barra: rótulo não selecionado/barra",
            "AlertDialog: texto/diálogo",
            "ListItem: texto de apoio/item",
            "OutlinedTextField: rótulo/fundo",
        ).forEach { esperado ->
            assertTrue("$esperado não acusado em $abaixo", abaixo.any { it.startsWith(esperado) })
        }
    }

    @Test
    fun todoComponenteDoMaterialImportadoNoAppTemParEfetivo() {
        iniciar()
        val cobertos = compor(Configuracao(TemaDiario.OFICINA, false)).map { it.componente }.toSet()
        val usados = componentesImportados()
        assertTrue("nenhum import do Material achado (caminho errado?)", usados.isNotEmpty())
        val semPar = usados.filter { it !in cobertos && it !in SEM_COR_PROPRIA }
        assertTrue("componente do Material sem par em ContrasteComponentesTest: $semPar", semPar.isEmpty())
        val exemptoCoberto = SEM_COR_PROPRIA.keys.filter { it in cobertos }
        assertTrue("isento que já tem par: $exemptoCoberto", exemptoCoberto.isEmpty())
    }

    @Test
    fun coresDeComponenteSoEntramPelasFuncoesQueOTesteLe() {
        // O teste mede os Defaults e as duas funções do app. Uma cor passada na chamada por outro caminho, ou a chamada
        // sem a função, deixaria o teste verde sobre uma cor que não é a da tela.
        val fontes = File("src/main/java").walkTopDown().filter { it.extension == "kt" }.toList()
        val passaCor = Regex("""\b(colors|containerColor|contentColor)\s*=""")
        val estranhas = fontes.flatMap { arquivo ->
            arquivo.readLines().withIndex().filter { (_, linha) ->
                passaCor.containsMatchIn(linha) && FUNCOES_DO_APP.none { it in linha }
            }.map { "${arquivo.name}:${it.index + 1}: ${it.value.trim()}" }
        }
        assertTrue("cor passada a componente fora das funções medidas: $estranhas", estranhas.isEmpty())
        CHAMADAS_COM_FUNCAO.forEach { (chamada, funcao) ->
            val sem = fontes.flatMap { arquivo ->
                val texto = arquivo.readText()
                Regex("""\b$chamada\(""").findAll(texto).map { texto.substring(it.range.first).take(JANELA_CHAMADA) }
            }.filterNot { "colors = $funcao" in it }
            assertTrue("$chamada chamado sem 'colors = $funcao': $sem", sem.isEmpty())
        }
    }

    private fun componentesImportados(): Set<String> {
        val raiz = File("src/main/java")
        val importa = Regex("""^import androidx\.compose\.material3\.(\*|[A-Za-z0-9_]+)""", RegexOption.MULTILINE)
        val nomes = raiz.walkTopDown().filter { it.extension == "kt" }
            .flatMap { arquivo -> importa.findAll(arquivo.readText()).map { it.groupValues[1] } }
            .toSet()
        assertTrue("import curinga do Material impede a conferência: use import por nome", "*" !in nomes)
        return nomes.map { ALIAS[it] ?: it }.toSet()
    }

    /** Pares de um componente; o nome do grupo é o do componente do Material (ou da barra do app). */
    private class Grupo(private val componente: String, private val saida: MutableList<ParEfetivo>) {
        private val rotulo = if (componente == "NavigationBarItem") "barra" else componente

        fun texto(nome: String, frente: Color, fundo: Color, papelFrente: Papel, papelFundo: Papel) =
            adicionar(nome, ContrasteWcag.MINIMO_TEXTO, frente, fundo, papelFrente, papelFundo)

        fun forma(nome: String, frente: Color, fundo: Color, papelFrente: Papel, papelFundo: Papel) =
            adicionar(nome, ContrasteWcag.MINIMO_COMPONENTE, frente, fundo, papelFrente, papelFundo)

        private fun adicionar(nome: String, minimo: Double, frente: Color, fundo: Color, pf: Papel, pu: Papel) {
            saida += ParEfetivo(componente, "$rotulo: $nome", frente, fundo, minimo, pf, pu)
        }
    }

    @Composable
    private fun coletarPares(): List<ParEfetivo> {
        val pares = mutableListOf<ParEfetivo>()
        fun grupo(componente: String, bloco: Grupo.() -> Unit) = Grupo(componente, pares).bloco()
        val pri = ColorScheme::primary
        val onPri = ColorScheme::onPrimary
        val bg = ColorScheme::background
        val sf = ColorScheme::surface
        val os = ColorScheme::onSurface
        val osv = ColorScheme::onSurfaceVariant
        val err = ColorScheme::error
        val barraPapel = ColorScheme::surfaceContainer
        val dialogoPapel = ColorScheme::surfaceContainerHigh
        val fundo = MaterialTheme.colorScheme.background

        // NavigationBar: container surfaceContainer; as cores do item vêm da função do app.
        val barra = coresBarraDestinos()
        val naBarra = NavigationBarDefaults.containerColor
        grupo("NavigationBarItem") {
            texto("rótulo selecionado/barra", barra.selectedTextColor, naBarra, pri, barraPapel)
            texto("rótulo não selecionado/barra", barra.unselectedTextColor, naBarra, osv, barraPapel)
            forma("ícone selecionado/barra", barra.selectedIconColor, naBarra, pri, barraPapel)
            forma("ícone não selecionado/barra", barra.unselectedIconColor, naBarra, osv, barraPapel)
            forma(
                "ícone selecionado/indicador",
                barra.selectedIconColor,
                barra.selectedIndicatorColor,
                pri,
                ColorScheme::secondaryContainer,
            )
        }

        // Botões. O contêiner transparente (Outlined e Text) está sobre o fundo da tela; no diálogo, sobre o do diálogo.
        val cheio = ButtonDefaults.buttonColors()
        val contorno = ButtonDefaults.outlinedButtonColors()
        val bordaBotao = ButtonDefaults.outlinedButtonBorder().corUnica()
        val simples = ButtonDefaults.textButtonColors()
        val dialogo = AlertDialogDefaults.containerColor
        val fab = FloatingActionButtonDefaults.containerColor
        val fabTexto = contentColorFor(fab)
        grupo("Button") {
            texto("texto/botão", cheio.contentColor, cheio.containerColor, onPri, pri)
            forma("botão/fundo", cheio.containerColor, fundo, pri, bg)
        }
        grupo("OutlinedButton") {
            texto("texto/fundo", contorno.contentColor, fundo, osv, bg)
            forma("borda/fundo", bordaBotao, fundo, ColorScheme::outlineVariant, bg)
        }
        grupo("TextButton") {
            texto("texto/fundo", simples.contentColor, fundo, pri, bg)
            texto("texto/diálogo", simples.contentColor, dialogo, pri, dialogoPapel)
        }
        grupo("ExtendedFloatingActionButton") {
            texto("texto/botão", fabTexto, fab, ColorScheme::onPrimaryContainer, ColorScheme::primaryContainer)
        }

        // FilterChip: contêiner não selecionado transparente (sobre o fundo). Os campos de cor são privados na versão instalada.
        val chip = FilterChipDefaults.filterChipColors()
        val bordaChip = FilterChipDefaults.filterChipBorder(enabled = true, selected = false).corUnica()
        grupo("FilterChip") {
            texto("rótulo não selecionado/fundo", chip.cor("labelColor"), fundo, osv, bg)
            texto(
                "rótulo selecionado/chip",
                chip.cor("selectedLabelColor"),
                chip.cor("selectedContainerColor"),
                ColorScheme::onSecondaryContainer,
                ColorScheme::secondaryContainer,
            )
            forma("borda não selecionado/fundo", bordaChip, fundo, ColorScheme::outlineVariant, bg)
        }

        val topo = TopAppBarDefaults.topAppBarColors()
        grupo("TopAppBar") {
            texto("título/barra", topo.titleContentColor, topo.containerColor, os, sf)
            forma("ícone de ação/barra", topo.actionIconContentColor, topo.containerColor, osv, sf)
            forma("ícone de navegação/barra", topo.navigationIconContentColor, topo.containerColor, os, sf)
        }

        val titulo = AlertDialogDefaults.titleContentColor
        val corpo = AlertDialogDefaults.textContentColor
        grupo("AlertDialog") {
            texto("título/diálogo", titulo, dialogo, os, dialogoPapel)
            texto("texto/diálogo", corpo, dialogo, osv, dialogoPapel)
        }

        val item = ListItemDefaults.colors()
        grupo("ListItem") {
            texto("título/item", item.headlineColor, item.containerColor, os, sf)
            texto("texto de apoio/item", item.supportingTextColor, item.containerColor, osv, sf)
            texto("sobretítulo/item", item.overlineColor, item.containerColor, osv, sf)
            forma("ícone inicial/item", item.leadingIconColor, item.containerColor, osv, sf)
            forma("ícone final/item", item.trailingIconColor, item.containerColor, osv, sf)
        }

        val chave = coresSwitchDiario()
        grupo("Switch") {
            forma("polegar ligado/trilho", chave.checkedThumbColor, chave.checkedTrackColor, onPri, pri)
            forma("trilho ligado/fundo", chave.checkedTrackColor, fundo, pri, bg)
            forma(
                "polegar desligado/trilho",
                chave.uncheckedThumbColor,
                chave.uncheckedTrackColor,
                osv,
                ColorScheme::surfaceContainerHighest,
            )
            forma("borda desligado/fundo", chave.uncheckedBorderColor, fundo, ColorScheme::outline, bg)
        }

        val caixa = CheckboxDefaults.colors()
        grupo("Checkbox") {
            forma("marca/caixa", caixa.checkedCheckmarkColor, caixa.checkedBoxColor, onPri, pri)
            forma("caixa marcada/fundo", caixa.checkedBoxColor, fundo, pri, bg)
            forma("borda desmarcada/fundo", caixa.uncheckedBorderColor, fundo, osv, bg)
        }

        val radio = RadioButtonDefaults.colors()
        grupo("RadioButton") {
            forma("selecionado/fundo", radio.selectedColor, fundo, pri, bg)
            forma("não selecionado/fundo", radio.unselectedColor, fundo, osv, bg)
        }

        val campo = OutlinedTextFieldDefaults.colors()
        grupo("OutlinedTextField") {
            texto("texto/fundo", campo.unfocusedTextColor, fundo, os, bg)
            texto("texto em foco/fundo", campo.focusedTextColor, fundo, os, bg)
            texto("rótulo/fundo", campo.unfocusedLabelColor, fundo, osv, bg)
            texto("rótulo em foco/fundo", campo.focusedLabelColor, fundo, pri, bg)
            texto("rótulo com erro/fundo", campo.errorLabelColor, fundo, err, bg)
            texto("texto de apoio/fundo", campo.unfocusedSupportingTextColor, fundo, osv, bg)
            texto("texto de apoio com erro/fundo", campo.errorSupportingTextColor, fundo, err, bg)
            forma("borda/fundo", campo.unfocusedIndicatorColor, fundo, ColorScheme::outline, bg)
            forma("borda em foco/fundo", campo.focusedIndicatorColor, fundo, pri, bg)
            forma("borda com erro/fundo", campo.errorIndicatorColor, fundo, err, bg)
        }

        val data = DatePickerDefaults.colors()
        val cd = data.containerColor
        grupo("DatePicker") {
            texto("título/seletor", data.titleContentColor, cd, osv, dialogoPapel)
            texto("cabeçalho/seletor", data.headlineContentColor, cd, osv, dialogoPapel)
            texto("dia da semana/seletor", data.weekdayContentColor, cd, os, dialogoPapel)
            texto("mês/seletor", data.subheadContentColor, cd, osv, dialogoPapel)
            forma("navegação/seletor", data.navigationContentColor, cd, osv, dialogoPapel)
            texto("dia/seletor", data.dayContentColor, cd, os, dialogoPapel)
            texto("dia selecionado/marca", data.selectedDayContentColor, data.selectedDayContainerColor, onPri, pri)
            forma("marca do dia selecionado/seletor", data.selectedDayContainerColor, cd, pri, dialogoPapel)
            texto("hoje/seletor", data.todayContentColor, cd, pri, dialogoPapel)
            forma("contorno de hoje/seletor", data.todayDateBorderColor, cd, pri, dialogoPapel)
            texto("ano/seletor", data.yearContentColor, cd, osv, dialogoPapel)
            texto("ano atual/seletor", data.currentYearContentColor, cd, pri, dialogoPapel)
            texto("ano selecionado/marca", data.selectedYearContentColor, data.selectedYearContainerColor, onPri, pri)
        }

        val arco = ProgressIndicatorDefaults.circularColor
        grupo("CircularProgressIndicator") { forma("arco/fundo", arco, fundo, pri, bg) }
        return pares
    }

    /** Cor da borda: o app só usa bordas de cor única. */
    private fun BorderStroke.corUnica(): Color = (brush as? SolidColor)?.value
        ?: error("borda com pincel que não é cor única: $brush")

    /** O Material 3 instalado não expõe as cores do chip; lê o campo e falha alto se ele sumir numa atualização. */
    private fun Any.cor(campo: String): Color {
        val f = javaClass.getDeclaredField(campo).apply { isAccessible = true }
        return Color(f.getLong(this).toULong())
    }

    private companion object {
        const val MOTIVO_MINIMO = 40
        const val JANELA_CHAMADA = 700

        /** As funções do app que escolhem cores de componente; o teste as lê. */
        val FUNCOES_DO_APP = listOf("coresBarraDestinos()", "coresSwitchDiario()")

        /** Chamada do Material → função do app que ela deve receber em `colors =`. */
        val CHAMADAS_COM_FUNCAO = mapOf(
            "NavigationBarItem" to "coresBarraDestinos()",
            "Switch" to "coresSwitchDiario()",
        )

        /** Nome do material 3 importado no app → nome do componente nos pares. */
        val ALIAS = mapOf(
            "NavigationBar" to "NavigationBarItem",
            "NavigationBarItemDefaults" to "NavigationBarItem",
            "NavigationBarItemColors" to "NavigationBarItem",
            "SwitchColors" to "Switch",
            "SwitchDefaults" to "Switch",
            "DatePickerDialog" to "DatePicker",
            "rememberDatePickerState" to "DatePicker",
        )

        /** Importados do Material que não têm cor própria a medir, cada um com o motivo. */
        val SEM_COR_PROPRIA = mapOf(
            "Text" to "cor herdada do ColorScheme: texto e ícone do app estão nos pares do ContrasteTemaTest",
            "Icon" to "cor herdada (LocalContentColor): coberta pelos pares do contêiner onde o ícone está",
            "MaterialTheme" to "só fornece o ColorScheme, a tipografia e as formas",
            "Typography" to "sem cor",
            "ColorScheme" to "é o próprio esquema medido pelo ContrasteTemaTest",
            "Scaffold" to "contêiner = background, medido nos pares do ContrasteTemaTest",
            "ExperimentalMaterial3Api" to "anotação, sem cor",
        )

        /**
         * Pares efetivos que ficam abaixo do mínimo de propósito (prefixo do nome → motivo escrito, ≥ 40 caracteres).
         * Cada um é conferido: se passar a passar, o teste pede para tirá-lo da lista.
         */
        val EXCECOES = mapOf(
            "OutlinedButton: borda/fundo" to
                "borda decorativa (outlineVariant, 1,35 a 1,61:1): o botão é identificado pelo rótulo, que passa de 4,5:1",
            "FilterChip: borda não selecionado/fundo" to
                "SINAL FRACO a corrigir em Campos.kt, fora da fronteira da frente #56: o chip selecionado só muda o " +
                "preenchimento (1,10 a 1,40:1) e perde a borda; pedir ao PM a borda em outline (3,17 a 3,83:1)",
        )
    }
}
