package com.pedrohrsm.diariodebordo.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Os 3 temas do Figma (arquivo XLfpBLh0ndZhvYecq2uYbz, página "Alta fidelidade — 3 temas"). */
enum class TemaDiario { OFICINA, ESTRADA, CROMO }

/**
 * Papéis de cor, nomeados pelo que a cor faz na tela. [variavelFigma] é o nome da variável na coleção
 * "Tema …" do Figma: o teste de tokens confere cada valor abaixo contra o export dessas variáveis.
 */
enum class PapelCor(val variavelFigma: String) {
    FUNDO("cor/bg"),
    SUPERFICIE("cor/surface"),
    SUPERFICIE_ALTA("cor/surface2"),
    NAVEGACAO("cor/nav"),
    LINHA("cor/line"),
    TEXTO("cor/ink"),
    TEXTO_SECUNDARIO("cor/ink2"),
    TEXTO_TERCIARIO("cor/ink3"),
    DESTAQUE("cor/accent"),
    DESTAQUE_SUAVE("cor/accentSoft"),
    SOBRE_DESTAQUE("cor/onAccent"),
    REALCE("cor/hi"),
    OK("cor/ok"),
    OK_SUAVE("cor/okSoft"),
    CRITICO("cor/crit"),
    CRITICO_SUAVE("cor/critSoft"),
    VEU("cor/scrim"),
}

/** Um token por papel, com o par completo em cada tema. Falta de papel reprova em `TokensDiarioTest`. */
typealias TokensDiario = Map<PapelCor, Color>

private val tokensOficina: TokensDiario = mapOf(
    PapelCor.FUNDO to Color(0xFF1A1C1E),
    PapelCor.SUPERFICIE to Color(0xFF24272A),
    PapelCor.SUPERFICIE_ALTA to Color(0xFF2E3236),
    PapelCor.NAVEGACAO to Color(0xFF202326),
    PapelCor.LINHA to Color(0xFF3A3F44),
    PapelCor.TEXTO to Color(0xFFF2F1EE),
    PapelCor.TEXTO_SECUNDARIO to Color(0xFFA8ADB2),
    PapelCor.TEXTO_TERCIARIO to Color(0xFF72787E),
    PapelCor.DESTAQUE to Color(0xFFFF6B1A),
    PapelCor.DESTAQUE_SUAVE to Color(0xFF3A2A20),
    PapelCor.SOBRE_DESTAQUE to Color(0xFF1A1C1E),
    PapelCor.REALCE to Color(0xFFFF6B1A),
    PapelCor.OK to Color(0xFF86AD66),
    PapelCor.OK_SUAVE to Color(0xFF26301F),
    PapelCor.CRITICO to Color(0xFFFF5A4E),
    PapelCor.CRITICO_SUAVE to Color(0xFF3D2323),
    PapelCor.VEU to Color(0xFF000000),
)

private val tokensEstrada: TokensDiario = mapOf(
    PapelCor.FUNDO to Color(0xFFF4EFE6),
    PapelCor.SUPERFICIE to Color(0xFFFFFDF8),
    PapelCor.SUPERFICIE_ALTA to Color(0xFFEAE3D6),
    PapelCor.NAVEGACAO to Color(0xFFEDE6DA),
    PapelCor.LINHA to Color(0xFFD8CFBF),
    PapelCor.TEXTO to Color(0xFF1E2A2F),
    PapelCor.TEXTO_SECUNDARIO to Color(0xFF55636A),
    PapelCor.TEXTO_TERCIARIO to Color(0xFF7D888E),
    PapelCor.DESTAQUE to Color(0xFF1F4E5F),
    PapelCor.DESTAQUE_SUAVE to Color(0xFFDCE7EA),
    PapelCor.SOBRE_DESTAQUE to Color(0xFFFFFFFF),
    PapelCor.REALCE to Color(0xFFC0532C),
    PapelCor.OK to Color(0xFF4E7D4A),
    PapelCor.OK_SUAVE to Color(0xFFE1ECDD),
    PapelCor.CRITICO to Color(0xFFB3261E),
    PapelCor.CRITICO_SUAVE to Color(0xFFF6DEDA),
    PapelCor.VEU to Color(0xFF1E2A2F),
)

private val tokensCromo: TokensDiario = mapOf(
    PapelCor.FUNDO to Color(0xFF12161C),
    PapelCor.SUPERFICIE to Color(0xFF1A2029),
    PapelCor.SUPERFICIE_ALTA to Color(0xFF232B36),
    PapelCor.NAVEGACAO to Color(0xFF161B22),
    PapelCor.LINHA to Color(0xFF2F3946),
    PapelCor.TEXTO to Color(0xFFE8ECF0),
    PapelCor.TEXTO_SECUNDARIO to Color(0xFF9AA6B2),
    PapelCor.TEXTO_TERCIARIO to Color(0xFF66727E),
    PapelCor.DESTAQUE to Color(0xFFC9D1D9),
    PapelCor.DESTAQUE_SUAVE to Color(0xFF2A323D),
    PapelCor.SOBRE_DESTAQUE to Color(0xFF12161C),
    PapelCor.REALCE to Color(0xFFC9D1D9),
    PapelCor.OK to Color(0xFF4CAF7A),
    PapelCor.OK_SUAVE to Color(0xFF1B2E26),
    PapelCor.CRITICO to Color(0xFFE76969),
    PapelCor.CRITICO_SUAVE to Color(0xFF3A1D20),
    PapelCor.VEU to Color(0xFF000000),
)

fun TemaDiario.tokens(): TokensDiario = when (this) {
    TemaDiario.OFICINA -> tokensOficina
    TemaDiario.ESTRADA -> tokensEstrada
    TemaDiario.CROMO -> tokensCromo
}

/**
 * Modo oficina (Figma: "textos secundários em contraste total"): o texto secundário e o terciário
 * passam a usar a cor do texto principal.
 */
fun TokensDiario.paraModoOficina(): TokensDiario = this + mapOf(
    PapelCor.TEXTO_SECUNDARIO to getValue(PapelCor.TEXTO),
    PapelCor.TEXTO_TERCIARIO to getValue(PapelCor.TEXTO),
)

/** Tema escuro quando o fundo é escuro: decide se os ícones das barras do sistema ficam claros ou escuros. */
fun TokensDiario.escuro(): Boolean = getValue(PapelCor.FUNDO).luminance() < 0.5f
