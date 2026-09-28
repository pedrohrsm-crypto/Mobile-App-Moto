package com.pedrohrsm.diariodebordo.core.preferencias

import kotlinx.coroutines.flow.Flow

/** Preferências do aparelho (não são dados do diário: não entram no arquivo exportado). */
interface Preferencias {
    /** Moto em foco nas abas Diário, Plano e Rotas. */
    val motoSelecionada: Flow<String?>
    fun selecionarMoto(id: String?)
}
