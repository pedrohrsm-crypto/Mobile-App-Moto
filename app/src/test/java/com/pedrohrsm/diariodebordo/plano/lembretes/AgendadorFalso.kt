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

/** Guarda os avisos que o sistema mostraria. Com [aceita] falso, simula o aparelho que não deixa notificar. */
class NotificadorFalso(var aceita: Boolean = true) : Notificador {
    val avisos = mutableListOf<Aviso>()
    override fun notificar(aviso: Aviso): Boolean {
        if (!aceita) return false
        avisos += aviso
        return true
    }
}
