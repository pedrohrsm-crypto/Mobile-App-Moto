package com.pedrohrsm.diariodebordo.rota.dominio

import com.pedrohrsm.diariodebordo.localizacao.Ponto
import kotlin.math.roundToLong

/**
 * Traçado salvo em texto compacto (algoritmo de polilinha codificada, precisão de 1e-5 grau, cerca de 1 m).
 * Guarda só latitude e longitude: o horário de cada ponto não sai do aparelho nem vai no arquivo exportado.
 */
object Polilinha {
    private const val FATOR = 1e5

    fun codificar(pontos: List<Ponto>): String = buildString {
        var latAnterior = 0L
        var lonAnterior = 0L
        pontos.forEach { p ->
            val lat = (p.latitude * FATOR).roundToLong()
            val lon = (p.longitude * FATOR).roundToLong()
            valor(lat - latAnterior)
            valor(lon - lonAnterior)
            latAnterior = lat
            lonAnterior = lon
        }
    }

    fun decodificar(texto: String): List<Ponto> {
        val pontos = mutableListOf<Ponto>()
        var i = 0
        var lat = 0L
        var lon = 0L
        fun proximo(): Long {
            var resultado = 0L
            var deslocamento = 0
            var b: Int
            do {
                require(i < texto.length) { "Polilinha incompleta" }
                b = texto[i++].code - 63
                resultado = resultado or ((b and 0x1f).toLong() shl deslocamento)
                deslocamento += 5
            } while (b >= 0x20)
            return if (resultado and 1L != 0L) (resultado shr 1).inv() else resultado shr 1
        }
        while (i < texto.length) {
            lat += proximo()
            lon += proximo()
            pontos += Ponto(lat / FATOR, lon / FATOR, instante = 0)
        }
        return pontos
    }

    private fun StringBuilder.valor(v: Long) {
        var n = if (v < 0) (v shl 1).inv() else v shl 1
        while (n >= 0x20) {
            append(((0x20 or (n and 0x1f).toInt()) + 63).toChar())
            n = n shr 5
        }
        append((n.toInt() + 63).toChar())
    }
}
