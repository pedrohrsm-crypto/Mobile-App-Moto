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
import com.pedrohrsm.diariodebordo.core.ui.bordaChipDiario
import com.pedrohrsm.diariodebordo.navegacao.coresBarraDestinos
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private typealias Papel = (ColorScheme) -> Color

/**
 * Contraste das cores EFETIVAS dos componentes do Material que o app usa (erro #55, 4ª e 5ª ocorrências da classe).
 * A lista de pares do [ContrasteTemaTest] é escrita à mão e não vê o que um componente usa por padrão. Aqui o
 * [DiarioTheme] é composto nos 3 temas × modo oficina e as cores saem dos `*Defaults` (ou das funções do app:
 * `coresBarraDestinos`, `coresSwitchDiario` e `bordaChipDiario`). O teste reprova de cinco modos:
 *  - par efetivo abaixo do mínimo (4,5:1 texto, 3:1 componente de interface);
 *  - cor efetiva diferente do papel do ColorScheme declarado no par (o padrão do Material mudou);
 *  - campo de cor de um `*Colors` em uso (fora de `disabled*`) sem par e sem isenção nomeada: cada cor lida carrega a
 *    origem `grupo/Classe.campo`, então apagar um par, ou o campo dele, deixa o campo descoberto;
 *  - cor solta (container do diálogo, borda do chip...) esperada em [SOLTAS_ESPERADAS] sem par;
 *  - componente do Material usado no app, importado ou qualificado, sem nenhum par.
 * Estados desabilitados ficam de fora: a WCAG os isenta.
 *
 * Fronteira conhecida (decidida com o PM em 28-Out-2026): a guarda de "cor só pela função do app" é estática, feita
 * por regex sobre o texto das fontes. Aceita só `colors = coresBarraDestinos()`, `colors = coresSwitchDiario()` e
 * `border = bordaChipDiario(...)`, seguidos de `,` ou `)`, e reprova `color =`, `tint =` e `.copy(` dentro das
 * chamadas de `NavigationBarItem`, `Switch` e `FilterChip`. O que ainda escapa a uma guarda por regex (cor trazida por
 * variável, por outra função, por um wrapper do componente ou montada fora da chamada) não é pego aqui; a segunda rede
 * são as capturas (`CapturasBarraTest`, `CapturasChipTest` e as telas de `fase1/`), conferidas por
 * `verifyRoborazziDebug`. Fora também: `Text(color = ...)` e `Icon(tint = ...)` fora desses três componentes, como o
 * `PlanoScreen.kt:170` (`Text(color = cor)` no `overlineContent` do `ListItem`), cobertos pelos pares do
 * [ContrasteTemaTest] (`error/surface`, `primary/surface`) e pelo `ListItem` lido dos Defaults.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ContrasteComponentesTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    /** Cor lida de um componente; [origem] diz de onde veio (nulo para papel do ColorScheme). */
    private class Cor(val valor: Color, val origem: String? = null)

    private class ParEfetivo(
        val componente: String,
        val nome: String,
        val frente: Cor,
        val fundo: Cor,
        val minimo: Double,
        val papelFrente: Papel,
        val papelFundo: Papel,
    ) {
        val razao: Double get() = ContrasteWcag.contraste(frente.valor, fundo.valor)
    }

    /** Resultado de uma composição: os pares e os `*Colors` lidos (grupo → objeto). */
    private class Coletado(val pares: List<ParEfetivo>, val colors: List<Pair<String, Any>>)

    private class Configuracao(val tema: TemaDiario, val modoOficina: Boolean, val tokens: TokensDiario? = null)

    private var configuracao by mutableStateOf(Configuracao(TemaDiario.OFICINA, false))
    private var coletado = Coletado(emptyList(), emptyList())
    private var esquemaAtual: ColorScheme? = null
    private var bordasChip: Pair<BorderStroke, BorderStroke>? = null

    @Composable
    private fun Coleta() {
        esquemaAtual = MaterialTheme.colorScheme
        bordasChip = bordaChipDiario(selecionado = false) to bordaChipDiario(selecionado = true)
        coletado = coletarPares()
    }

    private fun compor(nova: Configuracao): Coletado {
        configuracao = nova
        rule.waitForIdle()
        return coletado
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

    private fun todasAsConfiguracoes(bloco: (TemaDiario, Boolean) -> Unit) =
        TemaDiario.entries.forEach { tema -> listOf(false, true).forEach { oficina -> bloco(tema, oficina) } }

    private fun hex(cor: Color): String = "#%08X".format(cor.toArgb())

    /** Descrição de tudo que está errado nos pares da composição atual; vazia quando todos passam. */
    private fun falhas(pares: List<ParEfetivo>): List<String> {
        val esquema = checkNotNull(esquemaAtual)
        return pares.flatMap { par ->
            buildList {
                val frente = par.papelFrente(esquema)
                val fundo = par.papelFundo(esquema)
                if (par.frente.valor != frente) {
                    add("${par.nome}: frente efetiva ${hex(par.frente.valor)} difere do papel declarado ${hex(frente)}")
                }
                if (par.fundo.valor != fundo) {
                    add("${par.nome}: fundo efetivo ${hex(par.fundo.valor)} difere do papel declarado ${hex(fundo)}")
                }
                if (par.razao < par.minimo) add("%s %.2f:1 (mínimo %.1f)".format(par.nome, par.razao, par.minimo))
            }
        }
    }

    @Test
    fun todoParEfetivoDeComponenteAtingeOMinimoEmCadaTema() {
        iniciar()
        todasAsConfiguracoes { tema, modoOficina ->
            val pares = compor(Configuracao(tema, modoOficina)).pares
            assertTrue("nenhum par coletado", pares.isNotEmpty())
            val abaixo = falhas(pares).filterNot { falha -> EXCECOES.keys.any { falha.startsWith(it) } }
            assertTrue("$tema (modo oficina=$modoOficina) reprovou: $abaixo", abaixo.isEmpty())
        }
    }

    @Test
    fun todoCampoDeCorEmUsoTemParOuIsencaoNomeada() {
        iniciar()
        val coletado = compor(Configuracao(TemaDiario.OFICINA, false))
        val usadas = coletado.pares.flatMap { listOf(it.frente.origem, it.fundo.origem) }.filterNotNull().toSet()
        val campos = camposDeCor(coletado)
        val descobertos = campos.filter { it !in usadas && ISENCOES.none { isencao -> isencao.vale(it) } }
        assertTrue("campo de cor em uso sem par nem isenção nomeada: $descobertos", descobertos.isEmpty())
        val soltasUsadas = usadas.filter { it.startsWith("solta:") }.map { it.removePrefix("solta:") }.toSet()
        assertEquals("cores soltas com par diferem das esperadas", SOLTAS_ESPERADAS.toSet(), soltasUsadas)
        val frenteSemOrigem = coletado.pares.filter { it.frente.origem == null }.map { it.nome }
        assertTrue(
            "par cuja frente não veio de um componente (sem origem): $frenteSemOrigem",
            frenteSemOrigem.isEmpty(),
        )
        val isentosComPar = campos.filter { it in usadas && ISENCOES.any { isencao -> isencao.vale(it) } }
        assertTrue("isento que já tem par (tire a isenção): $isentosComPar", isentosComPar.isEmpty())
    }

    @Test
    fun isencoesNomeadasTemMotivoNaoSaoVelhasENaoEscondemCorVisivel() {
        iniciar()
        val colorsPorConfiguracao = mutableListOf<Coletado>()
        todasAsConfiguracoes { tema, modoOficina -> colorsPorConfiguracao += compor(Configuracao(tema, modoOficina)) }
        ISENCOES.forEach { isencao ->
            assertTrue("isenção '${isencao.padrao}' sem motivo", isencao.motivo.length >= MOTIVO_MINIMO)
            val casadas = camposDeCor(colorsPorConfiguracao.first()).filter { isencao.vale(it) }
            assertTrue("isenção '${isencao.padrao}' não casa com nenhum campo: remova", casadas.isNotEmpty())
            if (isencao.transparente) {
                colorsPorConfiguracao.forEach { c ->
                    casadas.forEach { campo ->
                        val cor = valorDoCampo(c, campo)
                        assertEquals("$campo isento como transparente mas visível: ${hex(cor)}", 0f, cor.alpha, 0f)
                    }
                }
            }
        }
        // Exceção de par: uma que passou a passar é lixo, e uma sem motivo escrito não vale.
        val falhasPorPar = mutableListOf<String>()
        todasAsConfiguracoes { tema, modoOficina ->
            falhasPorPar += falhas(compor(Configuracao(tema, modoOficina)).pares)
        }
        EXCECOES.forEach { (nome, motivo) ->
            assertTrue("exceção '$nome' sem motivo", motivo.length >= MOTIVO_MINIMO)
            assertTrue("exceção '$nome' já passa: remova da lista", falhasPorPar.any { it.startsWith(nome) })
        }
    }

    @Test
    fun indicadorDaBarraPassaDe3ParaUmSobreABarraEmCadaTema() {
        // Com a fonte grande a barra só tem ícones: o que mostra o destino aberto é o indicador (secondaryContainer no
        // padrão do Material: de 1,02 a 1,34:1 sobre a barra). O app usa primary, e o ícone selecionado fica sobre ele.
        iniciar()
        todasAsConfiguracoes { tema, modoOficina ->
            val par = compor(Configuracao(tema, modoOficina)).pares.first { it.nome == "barra: indicador/barra" }
            val msg = "$tema (oficina=$modoOficina) indicador %.2f:1".format(par.razao)
            assertTrue(msg, par.razao >= ContrasteWcag.MINIMO_COMPONENTE)
        }
    }

    @Test
    fun chipSelecionadoSeDistingueDoNaoSelecionadoEmCadaTema() {
        // No modo oficina o rótulo é igual nos dois estados e o preenchimento dá de 1,10 a 1,40:1: o sinal é a borda,
        // mais grossa e de outra cor no selecionado.
        iniciar()
        todasAsConfiguracoes { tema, modoOficina ->
            compor(Configuracao(tema, modoOficina))
            val (solto, marcado) = checkNotNull(bordasChip)
            val onde = "$tema (oficina=$modoOficina)"
            assertTrue("$onde borda do selecionado não é mais grossa", marcado.width > solto.width)
            val razao = ContrasteWcag.contraste(solto.corUnica(), marcado.corUnica())
            assertTrue("$onde bordas de cor parecida: %.2f:1".format(razao), razao >= CONTRASTE_ENTRE_BORDAS_DO_CHIP)
        }
    }

    @Test
    fun parPadraoDeOutroComponentePlantadoAbaixoDoMinimoReprova() {
        // Canário de classe (#55): TEXTO_SECUNDARIO rebaixado quebra, sem lista nenhuma, o rótulo não selecionado da
        // barra, o texto do diálogo, o ListItem e o campo de texto. Ninguém escreveu esses pares: saem dos Defaults.
        iniciar()
        val rebaixado = TemaDiario.ESTRADA.tokens() + (PapelCor.TEXTO_SECUNDARIO to Color(0xFF8A9499))
        val abaixo = falhas(compor(Configuracao(TemaDiario.ESTRADA, false, rebaixado)).pares)
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
    fun coresDeComponenteSoEntramPelasFuncoesQueOTesteLe() {
        // O teste mede os Defaults e as funções do app. Uma cor passada na chamada por outro caminho, ou a chamada
        // sem a função, deixaria o teste verde sobre uma cor que não é a da tela. Texto inteiro do arquivo, não linha.
        val fontes = fontesDoApp()
        val passaCor = Regex("""\b(colors|containerColor|contentColor|border)\s*=""")
        val estranhas = fontes.flatMap { arquivo ->
            val texto = arquivo.readText()
            passaCor.findAll(texto).map { texto.substring(it.range.first) }
                .filter { resto -> FORMAS_EXATAS.none { it.matchAt(resto, 0) != null } }
                .map { "${arquivo.name}: ${it.lineSequence().take(3).joinToString(" ").trim()}" }
        }
        assertTrue("cor passada a componente fora das formas exatas medidas: $estranhas", estranhas.isEmpty())
        val proibidas = Regex("""\b(color|tint)\s*=|\.copy\(""")
        CHAMADAS_COM_FUNCAO.forEach { (chamada, forma) ->
            val chamadas = fontes.flatMap { arquivo -> chamadasCompletas(arquivo.readText(), chamada) }
            assertTrue("nenhuma chamada de $chamada achada (nome mudou?)", chamadas.isNotEmpty())
            val semForma = chamadas.filterNot { forma.containsMatchIn(it) }
            assertTrue("$chamada chamado sem a forma exata '${forma.pattern}': $semForma", semForma.isEmpty())
            val porFora = chamadas.filter { proibidas.containsMatchIn(it) }
            assertTrue("$chamada com color =, tint = ou .copy( dentro da chamada: $porFora", porFora.isEmpty())
        }
    }

    /** Cada chamada `Nome(...)` inteira, com os parênteses balanceados (os slots do componente ficam dentro). */
    private fun chamadasCompletas(texto: String, nome: String): List<String> =
        Regex("""\b$nome\(""").findAll(texto).map { achado ->
            var nivel = 0
            var fim = -1
            for (i in achado.range.last until texto.length) {
                when (texto[i]) {
                    '(' -> nivel++
                    ')' -> if (--nivel == 0) {
                        fim = i
                        break
                    }
                }
            }
            check(fim > 0) { "$nome( sem parêntese de fechamento" }
            texto.substring(achado.range.first, fim + 1)
        }.toList()

    @Test
    fun todoComponenteDoMaterialUsadoNoAppTemParEfetivo() {
        iniciar()
        val cobertos = compor(Configuracao(TemaDiario.OFICINA, false)).pares.map { it.componente }.toSet()
        val usados = componentesDoMaterialUsados()
        assertTrue("nenhum uso do Material achado (caminho errado?)", usados.isNotEmpty())
        val semPar = usados.filter { it !in cobertos && it !in SEM_COR_PROPRIA }
        assertTrue("componente do Material sem par em ContrasteComponentesTest: $semPar", semPar.isEmpty())
        val exemptoCoberto = SEM_COR_PROPRIA.keys.filter { it in cobertos }
        assertTrue("isento que já tem par: $exemptoCoberto", exemptoCoberto.isEmpty())
    }

    /** Fontes do app em `src/main/java` e `src/main/kotlin`; falha se a lista vier vazia (caminho errado). */
    private fun fontesDoApp(): List<File> {
        val fontes = listOf("src/main/java", "src/main/kotlin").map(::File).filter { it.isDirectory }
            .flatMap { raiz -> raiz.walkTopDown().filter { it.extension == "kt" }.toList() }
        assertTrue("nenhuma fonte do app achada: o teste não está lendo o código", fontes.isNotEmpty())
        return fontes
    }

    private fun componentesDoMaterialUsados(): Set<String> {
        val importa = Regex("""^import androidx\.compose\.material3\.(\*|[A-Za-z0-9_]+)""", RegexOption.MULTILINE)
        val qualificado = Regex("""androidx\.compose\.material3\.""")
        val nomes = mutableSetOf<String>()
        fontesDoApp().forEach { arquivo ->
            val texto = arquivo.readText()
            importa.findAll(texto).forEach { nomes += it.groupValues[1] }
            val fora = texto.lines().filterNot { it.trimStart().startsWith("import ") }
                .filter { qualificado.containsMatchIn(it) }
            val msg = "${arquivo.name}: material3 qualificado fora de import (use import por nome): $fora"
            assertTrue(msg, fora.isEmpty())
        }
        assertTrue("import curinga do Material impede a conferência: use import por nome", "*" !in nomes)
        return nomes.map { ALIAS[it] ?: it }.toSet()
    }

    /** Campos de cor de todos os `*Colors` lidos, fora de `disabled*`, como `grupo/Classe.campo`. */
    private fun camposDeCor(coletado: Coletado): List<String> = coletado.colors.flatMap { (grupo, objeto) ->
        camposLong(objeto).map { "$grupo/${objeto.javaClass.simpleName}.${it.name}" }
    }

    private fun camposLong(objeto: Any) = objeto.javaClass.declaredFields
        .filter { it.type == java.lang.Long.TYPE && !it.name.startsWith("disabled") && !it.isSynthetic }
        .filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }

    private fun valorDoCampo(coletado: Coletado, id: String): Color {
        val objeto = coletado.colors.first { (g, o) -> id.startsWith("$g/${o.javaClass.simpleName}.") }.second
        return lerCampo(objeto, id.substringAfterLast('.'))
    }

    /** O Material 3 instalado não expõe todas as cores; lê o campo e falha alto se ele sumir numa atualização. */
    private fun lerCampo(objeto: Any, campo: String): Color {
        val f = objeto.javaClass.getDeclaredField(campo).apply { isAccessible = true }
        return Color(f.getLong(objeto).toULong())
    }

    /** Pares de um componente; o nome do grupo é o do componente do Material (ou da barra do app). */
    private class Grupo(private val componente: String, private val saida: MutableList<ParEfetivo>) {
        private val rotulo = if (componente == "NavigationBarItem") "barra" else componente

        fun texto(nome: String, frente: Cor, fundo: Cor, papelFrente: Papel, papelFundo: Papel) =
            adicionar(nome, ContrasteWcag.MINIMO_TEXTO, frente, fundo, papelFrente, papelFundo)

        fun forma(nome: String, frente: Cor, fundo: Cor, papelFrente: Papel, papelFundo: Papel) =
            adicionar(nome, ContrasteWcag.MINIMO_COMPONENTE, frente, fundo, papelFrente, papelFundo)

        private fun adicionar(nome: String, minimo: Double, frente: Cor, fundo: Cor, pf: Papel, pu: Papel) {
            saida += ParEfetivo(componente, "$rotulo: $nome", frente, fundo, minimo, pf, pu)
        }
    }

    /** Leitor de campos de um `*Colors`; cada leitura carrega a origem `grupo/Classe.campo`. */
    private inner class Fonte(private val grupo: String, private val objeto: Any) {
        operator fun get(campo: String) = Cor(lerCampo(objeto, campo), "$grupo/${objeto.javaClass.simpleName}.$campo")
    }

    @Composable
    private fun coletarPares(): Coletado {
        val pares = mutableListOf<ParEfetivo>()
        val colors = mutableListOf<Pair<String, Any>>()
        fun grupo(componente: String, bloco: Grupo.() -> Unit) = Grupo(componente, pares).bloco()
        fun fonte(grupo: String, objeto: Any): Fonte = Fonte(grupo, objeto).also { colors += grupo to objeto }
        fun solta(nome: String, cor: Color) = Cor(cor, "solta:$nome")
        val pri = ColorScheme::primary
        val onPri = ColorScheme::onPrimary
        val bg = ColorScheme::background
        val sf = ColorScheme::surface
        val os = ColorScheme::onSurface
        val osv = ColorScheme::onSurfaceVariant
        val err = ColorScheme::error
        val out = ColorScheme::outline
        val barraPapel = ColorScheme::surfaceContainer
        val dialogoPapel = ColorScheme::surfaceContainerHigh
        val fundo = Cor(MaterialTheme.colorScheme.background)

        // NavigationBar: container surfaceContainer; as cores do item vêm da função do app.
        val barra = fonte("NavigationBarItem", coresBarraDestinos())
        val naBarra = solta("NavigationBar/container", NavigationBarDefaults.containerColor)
        grupo("NavigationBarItem") {
            texto("rótulo selecionado/barra", barra["selectedTextColor"], naBarra, pri, barraPapel)
            texto("rótulo não selecionado/barra", barra["unselectedTextColor"], naBarra, osv, barraPapel)
            val indicador = barra["selectedIndicatorColor"]
            forma("ícone selecionado/indicador", barra["selectedIconColor"], indicador, onPri, pri)
            forma("indicador/barra", indicador, naBarra, pri, barraPapel)
            forma("ícone não selecionado/barra", barra["unselectedIconColor"], naBarra, osv, barraPapel)
        }

        // Botões. O contêiner transparente (Outlined e Text) está sobre o fundo da tela; no diálogo, sobre o do diálogo.
        val cheio = fonte("Button", ButtonDefaults.buttonColors())
        val contorno = fonte("OutlinedButton", ButtonDefaults.outlinedButtonColors())
        val bordaBotao = solta("OutlinedButton/borda", ButtonDefaults.outlinedButtonBorder().corUnica())
        val simples = fonte("TextButton", ButtonDefaults.textButtonColors())
        val simplesNoDialogo = fonte("TextButton em AlertDialog", ButtonDefaults.textButtonColors())
        val dialogo = solta("AlertDialog/container", AlertDialogDefaults.containerColor)
        val fab = solta("FAB/container", FloatingActionButtonDefaults.containerColor)
        val fabTexto = solta("FAB/conteúdo", contentColorFor(fab.valor))
        grupo("Button") {
            texto("texto/botão", cheio["contentColor"], cheio["containerColor"], onPri, pri)
            forma("botão/fundo", cheio["containerColor"], fundo, pri, bg)
        }
        grupo("OutlinedButton") {
            texto("texto/fundo", contorno["contentColor"], fundo, osv, bg)
            forma("borda/fundo", bordaBotao, fundo, ColorScheme::outlineVariant, bg)
        }
        grupo("TextButton") {
            texto("texto/fundo", simples["contentColor"], fundo, pri, bg)
            texto("texto/diálogo", simplesNoDialogo["contentColor"], dialogo, pri, dialogoPapel)
        }
        grupo("ExtendedFloatingActionButton") {
            texto("texto/botão", fabTexto, fab, ColorScheme::onPrimaryContainer, ColorScheme::primaryContainer)
        }

        // FilterChip: contêiner não selecionado transparente (sobre o fundo). Os campos de cor são privados na versão instalada.
        val chip = fonte("FilterChip", FilterChipDefaults.filterChipColors())
        val bordaChip = solta("FilterChip/borda não selecionado", bordaChipDiario(selecionado = false).corUnica())
        val bordaChipMarcado = solta("FilterChip/borda selecionado", bordaChipDiario(selecionado = true).corUnica())
        grupo("FilterChip") {
            texto("rótulo não selecionado/fundo", chip["labelColor"], fundo, osv, bg)
            texto(
                "rótulo selecionado/chip",
                chip["selectedLabelColor"],
                chip["selectedContainerColor"],
                ColorScheme::onSecondaryContainer,
                ColorScheme::secondaryContainer,
            )
            forma("borda não selecionado/fundo", bordaChip, fundo, out, bg)
            forma("borda selecionado/fundo", bordaChipMarcado, fundo, pri, bg)
        }

        val topo = fonte("TopAppBar", TopAppBarDefaults.topAppBarColors())
        grupo("TopAppBar") {
            texto("título/barra", topo["titleContentColor"], topo["containerColor"], os, sf)
            forma("ícone de ação/barra", topo["actionIconContentColor"], topo["containerColor"], osv, sf)
            forma("ícone de navegação/barra", topo["navigationIconContentColor"], topo["containerColor"], os, sf)
        }

        val titulo = solta("AlertDialog/título", AlertDialogDefaults.titleContentColor)
        val corpo = solta("AlertDialog/texto", AlertDialogDefaults.textContentColor)
        grupo("AlertDialog") {
            texto("título/diálogo", titulo, dialogo, os, dialogoPapel)
            texto("texto/diálogo", corpo, dialogo, osv, dialogoPapel)
        }

        val item = fonte("ListItem", ListItemDefaults.colors())
        grupo("ListItem") {
            texto("título/item", item["headlineColor"], item["containerColor"], os, sf)
            texto("texto de apoio/item", item["supportingTextColor"], item["containerColor"], osv, sf)
            texto("sobretítulo/item", item["overlineColor"], item["containerColor"], osv, sf)
            forma("ícone inicial/item", item["leadingIconColor"], item["containerColor"], osv, sf)
            forma("ícone final/item", item["trailingIconColor"], item["containerColor"], osv, sf)
        }

        val chave = fonte("Switch", coresSwitchDiario())
        grupo("Switch") {
            forma("polegar ligado/trilho", chave["checkedThumbColor"], chave["checkedTrackColor"], onPri, pri)
            forma("trilho ligado/fundo", chave["checkedTrackColor"], fundo, pri, bg)
            forma(
                "polegar desligado/trilho",
                chave["uncheckedThumbColor"],
                chave["uncheckedTrackColor"],
                osv,
                ColorScheme::surfaceContainerHighest,
            )
            forma("borda desligado/fundo", chave["uncheckedBorderColor"], fundo, out, bg)
        }

        val caixa = fonte("Checkbox", CheckboxDefaults.colors())
        grupo("Checkbox") {
            forma("marca/caixa", caixa["checkedCheckmarkColor"], caixa["checkedBoxColor"], onPri, pri)
            forma("caixa marcada/fundo", caixa["checkedBoxColor"], fundo, pri, bg)
            forma("borda marcada/fundo", caixa["checkedBorderColor"], fundo, pri, bg)
            forma("borda desmarcada/fundo", caixa["uncheckedBorderColor"], fundo, osv, bg)
        }

        val radio = fonte("RadioButton", RadioButtonDefaults.colors())
        grupo("RadioButton") {
            forma("selecionado/fundo", radio["selectedColor"], fundo, pri, bg)
            forma("não selecionado/fundo", radio["unselectedColor"], fundo, osv, bg)
        }

        val campo = fonte("OutlinedTextField", OutlinedTextFieldDefaults.colors())
        grupo("OutlinedTextField") {
            texto("texto/fundo", campo["unfocusedTextColor"], fundo, os, bg)
            texto("texto em foco/fundo", campo["focusedTextColor"], fundo, os, bg)
            texto("texto com erro/fundo", campo["errorTextColor"], fundo, os, bg)
            texto("rótulo/fundo", campo["unfocusedLabelColor"], fundo, osv, bg)
            texto("rótulo em foco/fundo", campo["focusedLabelColor"], fundo, pri, bg)
            texto("rótulo com erro/fundo", campo["errorLabelColor"], fundo, err, bg)
            texto("texto de apoio/fundo", campo["unfocusedSupportingTextColor"], fundo, osv, bg)
            texto("texto de apoio em foco/fundo", campo["focusedSupportingTextColor"], fundo, osv, bg)
            texto("texto de apoio com erro/fundo", campo["errorSupportingTextColor"], fundo, err, bg)
            forma("borda/fundo", campo["unfocusedIndicatorColor"], fundo, out, bg)
            forma("borda em foco/fundo", campo["focusedIndicatorColor"], fundo, pri, bg)
            forma("borda com erro/fundo", campo["errorIndicatorColor"], fundo, err, bg)
            forma("cursor/fundo", campo["cursorColor"], fundo, pri, bg)
            forma("cursor com erro/fundo", campo["errorCursorColor"], fundo, err, bg)
        }

        val data = fonte("DatePicker", DatePickerDefaults.colors())
        val cd = data["containerColor"]
        grupo("DatePicker") {
            texto("título/seletor", data["titleContentColor"], cd, osv, dialogoPapel)
            texto("cabeçalho/seletor", data["headlineContentColor"], cd, osv, dialogoPapel)
            texto("dia da semana/seletor", data["weekdayContentColor"], cd, os, dialogoPapel)
            texto("mês/seletor", data["subheadContentColor"], cd, osv, dialogoPapel)
            forma("navegação/seletor", data["navigationContentColor"], cd, osv, dialogoPapel)
            texto("dia/seletor", data["dayContentColor"], cd, os, dialogoPapel)
            val marca = data["selectedDayContainerColor"]
            texto("dia selecionado/marca", data["selectedDayContentColor"], marca, onPri, pri)
            forma("marca do dia selecionado/seletor", marca, cd, pri, dialogoPapel)
            texto("hoje/seletor", data["todayContentColor"], cd, pri, dialogoPapel)
            forma("contorno de hoje/seletor", data["todayDateBorderColor"], cd, pri, dialogoPapel)
            texto("ano/seletor", data["yearContentColor"], cd, osv, dialogoPapel)
            texto("ano atual/seletor", data["currentYearContentColor"], cd, pri, dialogoPapel)
            texto(
                "ano selecionado/marca",
                data["selectedYearContentColor"],
                data["selectedYearContainerColor"],
                onPri,
                pri,
            )
        }

        val arco = solta("Progress/arco", ProgressIndicatorDefaults.circularColor)
        grupo("CircularProgressIndicator") { forma("arco/fundo", arco, fundo, pri, bg) }
        return Coletado(pares, colors)
    }

    /** Cor da borda: o app só usa bordas de cor única. */
    private fun BorderStroke.corUnica(): Color = (brush as? SolidColor)?.value
        ?: error("borda com pincel que não é cor única: $brush")

    private class Isencao(val padrao: String, val motivo: String, val transparente: Boolean = false) {
        private val regex = Regex(padrao)

        fun vale(campo: String) = regex.matches(campo)
    }

    private companion object {
        const val MOTIVO_MINIMO = 20

        /** Medido entre as bordas do chip: de 1,30 (Cromo, modo oficina) a 3,18:1; o limiar fica abaixo do menor. */
        const val CONTRASTE_ENTRE_BORDAS_DO_CHIP = 1.2

        /**
         * As únicas formas aceitas de passar cor a um componente: a função do app, seguida de `,` ou `)`. Com `.copy(`
         * ou qualquer outra coisa depois, a cor deixa de ser a que o teste lê.
         */
        val FORMAS_EXATAS = listOf(
            Regex("""\bcolors\s*=\s*coresBarraDestinos\(\)\s*[,)]"""),
            Regex("""\bcolors\s*=\s*coresSwitchDiario\(\)\s*[,)]"""),
            Regex("""\bborder\s*=\s*bordaChipDiario\([^()]*\)\s*[,)]"""),
        )

        /** Chamada do Material → forma exata que ela deve conter. */
        val CHAMADAS_COM_FUNCAO = mapOf(
            "NavigationBarItem" to FORMAS_EXATAS[0],
            "Switch" to FORMAS_EXATAS[1],
            "FilterChip" to FORMAS_EXATAS[2],
        )

        /** Cores que não são campo de `*Colors` (getter solto ou borda) e que precisam de par. */
        val SOLTAS_ESPERADAS = listOf(
            "NavigationBar/container",
            "OutlinedButton/borda",
            "AlertDialog/container",
            "AlertDialog/título",
            "AlertDialog/texto",
            "FAB/container",
            "FAB/conteúdo",
            "FilterChip/borda não selecionado",
            "FilterChip/borda selecionado",
            "Progress/arco",
        )

        /** Nome do material 3 importado no app → nome do componente nos pares. */
        val ALIAS = mapOf(
            "NavigationBar" to "NavigationBarItem",
            "NavigationBarItemDefaults" to "NavigationBarItem",
            "NavigationBarItemColors" to "NavigationBarItem",
            "SwitchColors" to "Switch",
            "SwitchDefaults" to "Switch",
            "FilterChipDefaults" to "FilterChip",
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
         * Campos de `*Colors` em uso que ficam sem par, `grupo/Classe.campo`, cada um com o motivo. `transparente`
         * isenções são conferidas: o campo precisa ter alfa 0 nos 6 casos. Uma isenção que não casa com campo nenhum,
         * ou que casa com campo já coberto por par, reprova.
         */
        val ISENCOES = listOf(
            Isencao(
                """(OutlinedButton|TextButton|TextButton em AlertDialog)/ButtonColors\.containerColor""",
                "contêiner transparente: o par é contra o fundo da tela ou do diálogo",
                transparente = true,
            ),
            Isencao(
                """FilterChip/SelectableChipColors\.containerColor""",
                "contêiner do chip não selecionado transparente: o par é contra o fundo",
                transparente = true,
            ),
            Isencao(
                """Checkbox/CheckboxColors\.(uncheckedBoxColor|uncheckedCheckmarkColor)""",
                "caixa e marca desmarcadas transparentes: o que se vê é a borda desmarcada, com par",
                transparente = true,
            ),
            Isencao(
                """OutlinedTextField/TextFieldColors\.(focused|unfocused|error)ContainerColor""",
                "campo vazado (outlined): contêiner transparente, o par é contra o fundo",
                transparente = true,
            ),
            Isencao(
                """FilterChip/SelectableChipColors\.(leading|trailing|selectedLeading|selectedTrailing)IconColor""",
                "o app não põe ícone nos chips de escolha",
            ),
            Isencao(
                """Switch/SwitchColors\.(checked|unchecked)IconColor""",
                "o Switch do app não leva ícone no polegar",
            ),
            Isencao(
                """Switch/SwitchColors\.checkedBorderColor""",
                "ligado, o trilho é preenchido e a borda é a mesma cor dele (par do trilho)",
            ),
            Isencao(
                """OutlinedTextField/TextFieldColors\.(focused|unfocused|error)(Leading|Trailing)IconColor""",
                "os campos de texto do app não levam ícone",
            ),
            Isencao(
                """OutlinedTextField/TextFieldColors\.(focused|unfocused|error)(Placeholder|Prefix|Suffix)Color""",
                "os campos de texto do app não usam placeholder, prefixo nem sufixo",
            ),
            Isencao(
                """DatePicker/DatePickerColors\.dayInSelectionRange(Container|Content)Color""",
                "o seletor do app escolhe um dia só, sem intervalo",
            ),
            Isencao(
                """DatePicker/DatePickerColors\.dividerColor""",
                "divisória decorativa do seletor; a informação não depende dela",
            ),
            Isencao(
                """TopAppBar/TopAppBarColors\.(scrolledContainerColor|subtitleContentColor)""",
                "a barra do app não muda de cor ao rolar e não tem subtítulo",
            ),
        )

        /**
         * Pares efetivos que ficam abaixo do mínimo de propósito (prefixo do nome → motivo escrito).
         * Cada um é conferido: se passar a passar, o teste pede para tirá-lo da lista.
         */
        val EXCECOES = mapOf(
            "OutlinedButton: borda/fundo" to
                "borda decorativa (outlineVariant, 1,35 a 1,61:1): o botão é identificado pelo rótulo, que passa de 4,5:1",
        )
    }
}
