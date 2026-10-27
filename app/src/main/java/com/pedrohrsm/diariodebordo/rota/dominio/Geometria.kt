package com.pedrohrsm.diariodebordo.rota.dominio

import com.pedrohrsm.diariodebordo.localizacao.Ponto
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val RAIO_TERRA_M = 6_371_000.0

/** Leituras piores que isto (em metros) são ruído do GPS e ficam fora da rota. */
const val PRECISAO_MAXIMA_M = 50f

/** Tolerância da simplificação: desvios menores que isto somem do traçado salvo. */
const val TOLERANCIA_SIMPLIFICACAO_M = 10.0

/** Intervalo e distância mínima de captura: entre 2 e 5 s, para poupar bateria (plano de ação, 1.7). */
const val INTERVALO_CAPTURA_MS = 3_000L
const val DISTANCIA_MINIMA_M = 5f

private fun rad(graus: Double) = graus * PI / 180

/** Distância na superfície da Terra (haversine), em metros. */
fun distanciaM(a: Ponto, b: Ponto): Double {
    val dLat = rad(b.latitude - a.latitude)
    val dLon = rad(b.longitude - a.longitude)
    val h = sin(dLat / 2).pow(2) + cos(rad(a.latitude)) * cos(rad(b.latitude)) * sin(dLon / 2).pow(2)
    return 2 * RAIO_TERRA_M * asin(sqrt(h.coerceIn(0.0, 1.0)))
}

fun distanciaTotalM(pontos: List<Ponto>): Double = pontos.zipWithNext { a, b -> distanciaM(a, b) }.sum()

fun semRuido(pontos: List<Ponto>): List<Ponto> = pontos.filter { (it.precisaoM ?: 0f) <= PRECISAO_MAXIMA_M }

/**
 * Douglas-Peucker: mantém o formato da rota com bem menos pontos. Primeiro e último ficam sempre.
 * Distâncias numa projeção local plana, suficiente para a escala de um rolê.
 */
fun simplificar(pontos: List<Ponto>, toleranciaM: Double = TOLERANCIA_SIMPLIFICACAO_M): List<Ponto> {
    if (pontos.size < 3) return pontos
    val manter = BooleanArray(pontos.size)
    manter[0] = true
    manter[pontos.lastIndex] = true
    val pilha = ArrayDeque<Pair<Int, Int>>().apply { add(0 to pontos.lastIndex) }
    while (pilha.isNotEmpty()) {
        val (inicio, fim) = pilha.removeLast()
        var maior = 0.0
        var indice = -1
        for (i in inicio + 1 until fim) {
            val d = distanciaAoSegmentoM(pontos[i], pontos[inicio], pontos[fim])
            if (d > maior) {
                maior = d
                indice = i
            }
        }
        if (indice >= 0 && maior > toleranciaM) {
            manter[indice] = true
            pilha.add(inicio to indice)
            pilha.add(indice to fim)
        }
    }
    return pontos.filterIndexed { i, _ -> manter[i] }
}

private fun distanciaAoSegmentoM(p: Ponto, a: Ponto, b: Ponto): Double {
    val latRef = rad(a.latitude)
    fun x(q: Ponto) = rad(q.longitude - a.longitude) * cos(latRef) * RAIO_TERRA_M
    fun y(q: Ponto) = rad(q.latitude - a.latitude) * RAIO_TERRA_M
    val (bx, by) = x(b) to y(b)
    val (px, py) = x(p) to y(p)
    val comprimento2 = bx * bx + by * by
    if (comprimento2 == 0.0) return sqrt(px * px + py * py)
    val t = ((px * bx + py * by) / comprimento2).coerceIn(0.0, 1.0)
    val dx = px - t * bx
    val dy = py - t * by
    return sqrt(dx * dx + dy * dy)
}

/** Traçado projetado numa área de desenho (sem mapa de fundo), mantendo a proporção. */
fun projetar(pontos: List<Ponto>, largura: Float, altura: Float, margem: Float): List<Pair<Float, Float>> {
    if (pontos.isEmpty()) return emptyList()
    val latMedia = rad(pontos.map { it.latitude }.average())
    val xs = pontos.map { it.longitude * cos(latMedia) }
    val ys = pontos.map { it.latitude }
    val (minX, maxX) = xs.min() to xs.max()
    val (minY, maxY) = ys.min() to ys.max()
    val largUtil = largura - 2 * margem
    val altUtil = altura - 2 * margem
    val escala = minOf(
        if (maxX > minX) largUtil / (maxX - minX) else Double.MAX_VALUE,
        if (maxY > minY) altUtil / (maxY - minY) else Double.MAX_VALUE,
    ).let { if (it == Double.MAX_VALUE) 1.0 else it }
    val sobraX = (largUtil - (maxX - minX) * escala) / 2
    val sobraY = (altUtil - (maxY - minY) * escala) / 2
    return xs.indices.map { i ->
        val x = margem + sobraX + (xs[i] - minX) * escala
        // Norte para cima: y da tela cresce para baixo.
        val y = margem + sobraY + (maxY - ys[i]) * escala
        x.toFloat() to y.toFloat()
    }
}
