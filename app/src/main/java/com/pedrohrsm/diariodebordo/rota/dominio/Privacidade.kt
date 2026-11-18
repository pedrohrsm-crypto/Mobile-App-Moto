package com.pedrohrsm.diariodebordo.rota.dominio

import com.pedrohrsm.diariodebordo.localizacao.Ponto
import java.nio.ByteBuffer
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt
import kotlinx.serialization.Serializable

/**
 * Privacidade das rotas no arquivo exportado (#78, decisões do dono em 4-Nov-2026). O traçado completo fica no
 * aparelho; o que sai do aparelho perde as pontas da rota e tudo o que passa dentro de uma zona de privacidade.
 * É a única regra do que fica oculto: a exportação ([dividirTracado] via `paraOArquivo`) e o traçado em cinza do
 * detalhe da rota leem daqui.
 */

/** Tamanho do corte de cada ponta, escolhido em Ajustes. */
enum class CorteDasPontas(val metros: Int) {
    DESLIGADO(0),
    M500(500),
    KM1(1_000),
    KM2(2_000),
    KM5(5_000),
    ;

    companion object {
        val PADRAO = KM1
    }
}

/** O corte de cada rota varia até 20% para cima ou para baixo, para a borda não desenhar um círculo exato. */
const val VARIACAO_DO_CORTE = 0.2

/** Raios que a pessoa escolhe para uma zona. */
val RAIOS_DE_ZONA_M = listOf(200, 500, 1_000, 2_000)
const val RAIO_DE_ZONA_PADRAO_M = 500

/**
 * Círculo marcado pela pessoa (casa, trabalho). Fica só no aparelho: nunca vai no arquivo exportado.
 * O [toString] não leva o centro, para a coordenada não vazar numa mensagem de erro ou num relatório de falha.
 */
@Serializable
data class ZonaDePrivacidade(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val latitude: Double,
    val longitude: Double,
    val raioM: Int,
) {
    override fun toString() = "ZonaDePrivacidade(id=$id, raioM=$raioM)"
}

/** O que vale agora: o corte das pontas e as zonas. Ativa se qualquer um dos dois esconde alguma coisa. */
data class ProtecaoDasRotas(
    val corte: CorteDasPontas = CorteDasPontas.PADRAO,
    val zonas: List<ZonaDePrivacidade> = emptyList(),
) {
    val ativa: Boolean get() = corte != CorteDasPontas.DESLIGADO || zonas.isNotEmpty()

    /** Corte desta rota, em metros: o tamanho escolhido vezes o fator sorteado da rota ([fatorDoCorte]). */
    fun corteDaRotaM(segredo: ByteArray, rotaId: String): Double =
        if (corte == CorteDasPontas.DESLIGADO) 0.0 else corte.metros * fatorDoCorte(segredo, rotaId)
}

/**
 * Fator do corte de uma rota, entre 1 - [VARIACAO_DO_CORTE] e 1 + [VARIACAO_DO_CORTE]. O sorteio é o [segredo],
 * feito uma vez por aparelho e guardado; o fator sai dele e do id da rota (HMAC-SHA256). Assim a mesma rota tem
 * sempre o mesmo corte, sem coluna nova no banco, e quem tem só o arquivo não consegue refazer a conta.
 */
fun fatorDoCorte(segredo: ByteArray, rotaId: String): Double {
    require(segredo.isNotEmpty()) { "Segredo do corte vazio" }
    val mac = Mac.getInstance(HMAC).apply { init(SecretKeySpec(segredo, HMAC)) }
    val bits = ByteBuffer.wrap(mac.doFinal(rotaId.toByteArray())).long ushr 11
    val uniforme = bits.toDouble() / (1L shl 53)
    return 1 - VARIACAO_DO_CORTE + 2 * VARIACAO_DO_CORTE * uniforme
}

private const val HMAC = "HmacSHA256"

/** Um pedaço do traçado: o que vai no arquivo ([oculto] = false) ou o que fica só no aparelho. */
data class Trecho(val pontos: List<Ponto>, val oculto: Boolean)

/**
 * Folga somada a cada raio no corte: cobre o arredondamento da polilinha (1e-5 grau, cerca de 1 m) e a diferença
 * entre a projeção plana usada aqui e a distância na esfera. Nenhum ponto do arquivo cai dentro de um círculo.
 */
private const val FOLGA_M = 10.0

private const val RAIO_TERRA_M = 6_371_000.0

private data class Circulo(val latitude: Double, val longitude: Double, val raioM: Double)

/**
 * Divide o traçado ([trechos] na ordem da rota; um traçado já cortado chega com mais de um) em pedaços visíveis e
 * ocultos. Fica oculto tudo o que passa a menos de [corteM] do começo ou do fim da rota, e tudo o que passa dentro
 * de uma das [zonas], esteja onde estiver. Um segmento que atravessa um círculo é cortado na borda: o arquivo nunca
 * desenha uma reta por dentro de uma zona. Os pedaços visíveis e ocultos se alternam e se tocam na borda.
 */
fun dividirTracado(trechos: List<List<Ponto>>, corteM: Double, zonas: List<ZonaDePrivacidade>): List<Trecho> {
    val validos = trechos.filter { it.isNotEmpty() }
    if (validos.isEmpty()) return emptyList()
    val circulos = buildList {
        if (corteM > 0) {
            val inicio = validos.first().first()
            val fim = validos.last().last()
            add(Circulo(inicio.latitude, inicio.longitude, corteM + FOLGA_M))
            add(Circulo(fim.latitude, fim.longitude, corteM + FOLGA_M))
        }
        zonas.forEach { add(Circulo(it.latitude, it.longitude, it.raioM + FOLGA_M)) }
    }
    return validos.flatMap { dividirUmTrecho(it, circulos) }
}

/**
 * O traçado salvo de uma rota, dividido pela proteção de agora: o detalhe da rota mostra os pedaços ocultos em
 * cinza, e a exportação leva só os visíveis ([visiveis]). Proteção desligada e sem zonas: tudo visível.
 */
fun ProtecaoDasRotas.tracadoDaRota(rotaId: String, polilinha: String, segredo: ByteArray): List<Trecho> {
    val trechos = Polilinha.decodificarTrechos(polilinha)
    if (!ativa) return trechos.filter { it.isNotEmpty() }.map { Trecho(it, oculto = false) }
    return dividirTracado(trechos, corteDaRotaM(segredo, rotaId), zonas)
}

/** Só o que vai no arquivo: os pedaços visíveis com pelo menos dois pontos (um ponto solto não é traçado). */
fun List<Trecho>.visiveis(): List<List<Ponto>> = filter { !it.oculto && it.pontos.size >= 2 }.map { it.pontos }

private fun dividirUmTrecho(pontos: List<Ponto>, circulos: List<Circulo>): List<Trecho> {
    if (pontos.size == 1) return listOf(Trecho(pontos, oculto = circulos.any { it.contem(pontos[0]) }))
    val saida = mutableListOf<Trecho>()
    var atual = mutableListOf<Ponto>()
    var atualOculto = false
    pontos.zipWithNext { a, b ->
        pedacos(a, b, circulos).forEach { (t0, t1, oculto) ->
            val p0 = interpolar(a, b, t0)
            val p1 = interpolar(a, b, t1)
            if (atual.isNotEmpty() && atualOculto == oculto) {
                atual += p1
            } else {
                if (atual.isNotEmpty()) saida += Trecho(atual, atualOculto)
                atual = mutableListOf(p0, p1)
                atualOculto = oculto
            }
        }
    }
    if (atual.isNotEmpty()) saida += Trecho(atual, atualOculto)
    return saida
}

/** Partes do segmento a→b (em t, de 0 a 1) e se cada uma está dentro de algum círculo. */
private fun pedacos(a: Ponto, b: Ponto, circulos: List<Circulo>): List<Triple<Double, Double, Boolean>> {
    val dentro = circulos.mapNotNull { it.intervaloDentro(a, b) }.sortedBy { it.first }
    val unidos = mutableListOf<DoubleArray>()
    dentro.forEach { (ini, fim) ->
        val ultimo = unidos.lastOrNull()
        if (ultimo != null && ini <= ultimo[1]) ultimo[1] = maxOf(ultimo[1], fim) else unidos += doubleArrayOf(ini, fim)
    }
    val partes = mutableListOf<Triple<Double, Double, Boolean>>()
    var t = 0.0
    unidos.forEach { (ini, fim) ->
        if (ini - t > EPSILON) partes += Triple(t, ini, false)
        partes += Triple(ini, fim, true)
        t = fim
    }
    if (1.0 - t > EPSILON || partes.isEmpty()) partes += Triple(t, 1.0, false)
    return partes
}

private const val EPSILON = 1e-9

private fun interpolar(a: Ponto, b: Ponto, t: Double): Ponto = when {
    t <= 0.0 -> a
    t >= 1.0 -> b
    else -> Ponto(a.latitude + (b.latitude - a.latitude) * t, a.longitude + (b.longitude - a.longitude) * t, 0)
}

private fun rad(graus: Double) = graus * PI / 180

/** Projeção plana em metros, centrada no círculo. A interpolação em graus é uma reta nesta projeção. */
private fun Circulo.xy(p: Ponto): Pair<Double, Double> =
    rad(p.longitude - longitude) * cos(rad(latitude)) * RAIO_TERRA_M to rad(p.latitude - latitude) * RAIO_TERRA_M

private fun Circulo.contem(p: Ponto): Boolean {
    val (x, y) = xy(p)
    return x * x + y * y < raioM * raioM
}

/** Trecho de a→b (t de 0 a 1) dentro do círculo, ou null se o segmento não entra nele. */
private fun Circulo.intervaloDentro(a: Ponto, b: Ponto): Pair<Double, Double>? {
    val (ax, ay) = xy(a)
    val (bx, by) = xy(b)
    val dx = bx - ax
    val dy = by - ay
    val qa = dx * dx + dy * dy
    val qc = ax * ax + ay * ay - raioM * raioM
    if (qa == 0.0) return if (qc < 0) 0.0 to 1.0 else null
    val qb = 2 * (ax * dx + ay * dy)
    val discriminante = qb * qb - 4 * qa * qc
    if (discriminante <= 0) return null
    val raiz = sqrt(discriminante)
    val t1 = ((-qb - raiz) / (2 * qa)).coerceAtLeast(0.0)
    val t2 = ((-qb + raiz) / (2 * qa)).coerceAtMost(1.0)
    return if (t2 - t1 > EPSILON) t1 to t2 else null
}

/** Ponta de uma rota gravada onde a pessoa marca uma zona, sem GPS. */
enum class PontaDaRota { INICIO, FIM }

/** O ponto salvo da ponta pedida, ou null para rota sem traçado. Rota já cortada: a ponta do que sobrou. */
fun pontoDaPonta(polilinha: String, ponta: PontaDaRota): Ponto? {
    val trechos = Polilinha.decodificarTrechos(polilinha).filter { it.isNotEmpty() }
    if (trechos.isEmpty()) return null
    return when (ponta) {
        PontaDaRota.INICIO -> trechos.first().first()
        PontaDaRota.FIM -> trechos.last().last()
    }
}
