package com.pedrohrsm.diariodebordo.core.designsystem.theme

import androidx.compose.runtime.saveable.listSaver

/** Escolha do usuário: qual dos 3 temas e se o modo oficina está ligado. Troca em tempo real, sem recriar a Activity. */
data class AjusteTema(val tema: TemaDiario = TemaDiario.OFICINA, val modoOficina: Boolean = false) {
    companion object {
        val Saver = listSaver<AjusteTema, Any>(
            save = { listOf(it.tema.name, it.modoOficina) },
            restore = { AjusteTema(TemaDiario.valueOf(it[0] as String), it[1] as Boolean) },
        )
    }
}
