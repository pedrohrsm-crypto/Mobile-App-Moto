package com.pedrohrsm.diariodebordo.numeros

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Formatador único de número e unidade do app. Nenhuma tela monta "km", "R$" ou "L" à mão:
 * tudo passa por aqui, para o mesmo valor aparecer igual em toda parte.
 * Unidades de armazenamento: km em Int, dinheiro em centavos, volume em mililitros,
 * distância de rota em metros, duração em segundos, instantes em milissegundos UTC.
 */
object Formatador {
    /** Espaço não separável entre número e unidade. */
    const val NBSP = '\u00A0'

    private val simbolos = DecimalFormatSymbols(Locale.forLanguageTag("pt-BR"))
    private val inteiro = DecimalFormat("#,##0", simbolos)
    private val duasCasas = DecimalFormat("#,##0.00", simbolos)
    private val umaCasa = DecimalFormat("#,##0.0", simbolos)
    private val data = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dataHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    fun km(km: Int): String = "${inteiro.format(km)} km"

    fun dinheiro(centavos: Long): String {
        val sinal = if (centavos < 0) "-" else ""
        return "${sinal}R$ ${duasCasas.format(abs(centavos) / 100.0)}"
    }

    fun litros(mililitros: Long): String = "${duasCasas.format(mililitros / 1000.0)} L"

    /** Contagem simples com ponto de milhar: "1.284". */
    fun quantidade(n: Long): String = inteiro.format(n)

    /**
     * Distância de rota: metros abaixo de 1 km, km com uma casa a partir dele. Número e unidade unidos por espaço
     * não separável ([NBSP]): com fonte grande a unidade não desce sozinha para a linha de baixo.
     */
    fun distancia(metros: Long): String =
        if (metros < 1000) "${inteiro.format(metros)}${NBSP}m" else "${umaCasa.format(metros / 1000.0)}${NBSP}km"

    /** Duração de rota: "45 min" ou "1 h 05 min", com espaço não separável entre número e unidade. */
    fun duracao(segundos: Long): String {
        val h = segundos / 3600
        val min = (segundos % 3600) / 60
        val minutos = "$min${NBSP}min"
        return if (h > 0) String.format(Locale.ROOT, "%d${NBSP}h %02d${NBSP}min", h, min) else minutos
    }

    fun data(millis: Long, fuso: ZoneId): String = data.format(Instant.ofEpochMilli(millis).atZone(fuso))

    fun dataHora(millis: Long, fuso: ZoneId): String = dataHora.format(Instant.ofEpochMilli(millis).atZone(fuso))

    /** Dia do calendário, sem fuso: para datas calculadas, como a próxima revisão do plano. */
    fun data(dia: LocalDate): String = data.format(dia)

    /** Fuso gravado no registro; se estiver vazio ou inválido, o do aparelho. */
    fun fuso(id: String): ZoneId = runCatching { ZoneId.of(id) }.getOrDefault(ZoneId.systemDefault())

    /** Local do registro na tela (5 casas, cerca de 1 m). Só na interface: coordenada nunca vai para log. */
    fun coordenada(latitude: Double, longitude: Double): String =
        String.format(Locale.ROOT, "%.5f, %.5f", latitude, longitude)
}

/** Leitura do que a pessoa digitou, no formato brasileiro. Null quando o texto não é válido. */
object Leitor {
    private fun limpar(texto: String) = texto.trim().replace(" ", "").replace("R$", "")

    /** "12.345" ou "12345" → 12345. Aceita só inteiros de 0 a 9.999.999. */
    fun km(texto: String): Int? {
        val t = limpar(texto).removeSuffix("km").replace(".", "")
        if (t.isEmpty() || !t.all { it.isDigit() } || t.length > 7) return null
        return t.toInt()
    }

    /** "1.234,56", "1234,5", "10" → centavos. */
    fun dinheiro(texto: String): Long? = decimal(limpar(texto), casas = 2)

    /** "12,5" (litros) → 12500 mililitros. */
    fun litros(texto: String): Long? = decimal(limpar(texto).removeSuffix("L").removeSuffix("l"), casas = 3)

    private fun decimal(t: String, casas: Int): Long? {
        if (t.isEmpty()) return null
        val partes = t.replace(".", "").split(",")
        if (partes.size > 2) return null
        val inteira = partes[0].ifEmpty { "0" }
        val fracao = partes.getOrElse(1) { "" }
        if (!inteira.all { it.isDigit() } || !fracao.all { it.isDigit() } || fracao.length > casas) return null
        if (inteira.length > 12) return null
        var fator = 1L
        repeat(casas) { fator *= 10 }
        return inteira.toLong() * fator + fracao.padEnd(casas, '0').toLong()
    }
}

/** Texto para preencher um campo em edição, no formato que o [Leitor] lê de volta. */
object TextoDeNumero {
    fun dinheiro(centavos: Long): String = String.format(Locale.ROOT, "%d,%02d", centavos / 100, abs(centavos % 100))

    fun litros(mililitros: Long): String {
        val fracao = String.format(Locale.ROOT, "%03d", mililitros % 1000).trimEnd('0')
        return if (fracao.isEmpty()) "${mililitros / 1000}" else "${mililitros / 1000},$fracao"
    }
}
