package com.pedrohrsm.diariodebordo.plano.dominio

/**
 * Regra pura do plano de manutenção: o que vence primeiro, km ou tempo.
 * Sem Android aqui, para ser testada na JVM.
 */
data class Intervalo(val km: Int?, val dias: Int?) {
    init {
        require(km != null || dias != null) { "Intervalo precisa de km, de dias ou dos dois" }
        require(km == null || km > 0) { "Intervalo em km precisa ser positivo" }
        require(dias == null || dias > 0) { "Intervalo em dias precisa ser positivo" }
    }
}

data class Situacao(val kmRestantes: Int?, val diasRestantes: Int?) {
    val vencida: Boolean
        get() = (kmRestantes != null && kmRestantes <= 0) || (diasRestantes != null && diasRestantes <= 0)
}

fun situacaoDaManutencao(intervalo: Intervalo, kmNaUltimaVez: Int, kmAtual: Int, diasDesdeUltimaVez: Int): Situacao {
    require(kmAtual >= kmNaUltimaVez) { "Hodômetro atual menor que o da última manutenção" }
    return Situacao(
        kmRestantes = intervalo.km?.let { it - (kmAtual - kmNaUltimaVez) },
        diasRestantes = intervalo.dias?.let { it - diasDesdeUltimaVez },
    )
}
