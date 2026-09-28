package com.pedrohrsm.diariodebordo.core.preferencias

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePreferencias : Preferencias {
    private val selecionada = MutableStateFlow<String?>(null)
    override val motoSelecionada: Flow<String?> = selecionada
    override fun selecionarMoto(id: String?) {
        selecionada.value = id
    }
}
