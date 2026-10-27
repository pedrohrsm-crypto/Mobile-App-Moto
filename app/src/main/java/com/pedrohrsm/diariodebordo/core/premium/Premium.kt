package com.pedrohrsm.diariodebordo.core.premium

import com.pedrohrsm.diariodebordo.garagem.dominio.LIMITE_MOTOS_GRATIS

/** Tudo que é pago fica atrás desta lista. No MVP a flag vem desligada (BuildConfig.PREMIUM_HABILITADO). */
enum class RecursoPremium { MOTOS_ILIMITADAS, BACKUP_NUVEM, RELATORIO_PDF, ESTATISTICAS }

/**
 * Único lugar que lê a flag do premium (1.10): as telas perguntam aqui, nunca ao BuildConfig. Com a flag
 * desligada nenhum recurso pago aparece, e o limite de motos é o do grátis.
 */
class Premium(val habilitado: Boolean) {

    fun disponivel(recurso: RecursoPremium): Boolean = habilitado

    fun recursosVisiveis(): Set<RecursoPremium> = RecursoPremium.entries.filterTo(mutableSetOf(), ::disponivel)

    /** Grátis: [LIMITE_MOTOS_GRATIS]. Premium: sem limite (null). A conta fica em `podeCadastrarMoto`. */
    val limiteDeMotos: Int?
        get() = if (disponivel(RecursoPremium.MOTOS_ILIMITADAS)) null else LIMITE_MOTOS_GRATIS
}
