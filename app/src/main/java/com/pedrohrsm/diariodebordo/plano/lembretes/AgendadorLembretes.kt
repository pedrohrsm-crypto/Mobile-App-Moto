package com.pedrohrsm.diariodebordo.plano.lembretes

/** Agenda a verificação dos lembretes. Interface para os repositórios serem testados na JVM. */
interface AgendadorLembretes {
    /** Verificação diária (lembretes por tempo), mantida pelo sistema mesmo após reiniciar o aparelho. */
    fun agendarDiario()

    /** Recalcula tudo numa tarefa só, logo depois de uma mudança de hodômetro ou do plano. */
    fun recalcularAgora()
}
