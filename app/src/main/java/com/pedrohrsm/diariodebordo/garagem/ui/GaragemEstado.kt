package com.pedrohrsm.diariodebordo.garagem.ui

import com.pedrohrsm.diariodebordo.garagem.data.Moto

sealed interface GaragemEstado {
    data object Carregando : GaragemEstado

    data class Pronto(val motos: List<Moto>) : GaragemEstado
}
