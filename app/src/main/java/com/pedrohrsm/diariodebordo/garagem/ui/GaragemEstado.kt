package com.pedrohrsm.diariodebordo.garagem.ui

import com.pedrohrsm.diariodebordo.garagem.data.Moto

sealed interface GaragemEstado {
    data object Carregando : GaragemEstado

    data class Pronto(
        val motos: List<Moto>,
        /** Km atual por id de moto; ausente quando a moto ainda não tem leitura. */
        val kmPorMoto: Map<String, Int> = emptyMap(),
        /** Falso quando o plano grátis já tem a moto que permite. */
        val podeAdicionar: Boolean = true,
    ) : GaragemEstado

    data class Erro(val detalhe: String?) : GaragemEstado
}
