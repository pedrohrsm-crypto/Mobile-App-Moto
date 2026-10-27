package com.pedrohrsm.diariodebordo.garagem.dominio

/** Regra única do hodômetro: o km nunca volta. Sem Android aqui, para ser testada na JVM. */
sealed interface ValidacaoLeitura {
    data object Aceita : ValidacaoLeitura
    data object Negativa : ValidacaoLeitura
    data object AcimaDoLimite : ValidacaoLeitura
    data class MenorQueAnterior(val anteriorKm: Int) : ValidacaoLeitura
    data class IgualAnterior(val anteriorKm: Int) : ValidacaoLeitura
}

const val KM_MAXIMO = 9_999_999

fun validarLeitura(anteriorKm: Int?, novoKm: Int): ValidacaoLeitura = when {
    novoKm < 0 -> ValidacaoLeitura.Negativa
    novoKm > KM_MAXIMO -> ValidacaoLeitura.AcimaDoLimite
    anteriorKm == null -> ValidacaoLeitura.Aceita
    novoKm < anteriorKm -> ValidacaoLeitura.MenorQueAnterior(anteriorKm)
    novoKm == anteriorKm -> ValidacaoLeitura.IgualAnterior(anteriorKm)
    else -> ValidacaoLeitura.Aceita
}

class LeituraRecusadaException(val motivo: ValidacaoLeitura) :
    IllegalArgumentException("Leitura de hodômetro recusada: $motivo")
