package com.pedrohrsm.diariodebordo.core.premium

/** Tudo que é pago fica atrás desta lista. No MVP a flag vem desligada (BuildConfig.PREMIUM_HABILITADO). */
enum class RecursoPremium { MOTOS_ILIMITADAS, BACKUP_NUVEM, RELATORIO_PDF, ESTATISTICAS }

/** Única regra que decide o que o plano grátis mostra: nenhuma tela consulta a flag direto. */
class Premium(val habilitado: Boolean) {

    fun disponivel(recurso: RecursoPremium): Boolean = habilitado

    fun recursosVisiveis(): Set<RecursoPremium> = RecursoPremium.entries.filterTo(mutableSetOf()) { disponivel(it) }

    /** Grátis: 1 moto. Premium: sem limite (null). */
    val limiteDeMotos: Int?
        get() = if (disponivel(RecursoPremium.MOTOS_ILIMITADAS)) null else LIMITE_GRATIS

    fun podeCadastrarMoto(motosAtivas: Int): Boolean = limiteDeMotos?.let { motosAtivas < it } ?: true

    companion object {
        const val LIMITE_GRATIS = 1
    }
}

class LimiteDeMotosException(val limite: Int) : IllegalStateException("O plano grátis permite $limite moto")
