package com.pedrohrsm.diariodebordo.plano.lembretes

class AgendadorFalso : AgendadorLembretes {
    var diarios = 0
    var recalculos = 0

    /** Quando preenchido, `recalcularAgora` lança esta exceção (o WorkManager indisponível). */
    var falha: Throwable? = null
    override fun agendarDiario() {
        diarios++
    }
    override fun recalcularAgora() {
        falha?.let { throw it }
        recalculos++
    }
}

/** Guarda os avisos que o sistema mostraria. Com [aceita] falso, simula o aparelho que não deixa notificar. */
class NotificadorFalso(var aceita: Boolean = true) : Notificador {
    val avisos = mutableListOf<Aviso>()

    /** Roda no meio do envio, para simular o que muda no banco enquanto o aviso é mostrado. */
    var aoNotificar: (Aviso) -> Unit = {}
    override fun notificar(aviso: Aviso): Boolean {
        if (!aceita) return false
        aoNotificar(aviso)
        avisos += aviso
        return true
    }
}
