package com.pedrohrsm.diariodebordo.plano.lembretes

class AgendadorFalso : AgendadorLembretes {
    var diarios = 0
    var recalculos = 0
    override fun agendarDiario() {
        diarios++
    }
    override fun recalcularAgora() {
        recalculos++
    }
}
