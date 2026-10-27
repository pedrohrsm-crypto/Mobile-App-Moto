package com.pedrohrsm.diariodebordo.plano.lembretes

/** Mostra o aviso ao usuário. Interface para a verificação ser testada na JVM, sem notificação de verdade. */
interface Notificador {
    /** Devolve false quando o aparelho não deixa notificar: o aviso fica para a próxima verificação. */
    fun notificar(aviso: Aviso): Boolean
}
