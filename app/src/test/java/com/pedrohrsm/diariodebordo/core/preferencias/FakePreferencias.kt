package com.pedrohrsm.diariodebordo.core.preferencias

import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakePreferencias(inicial: AjusteTema = AjusteTema()) : Preferencias {
    override val ajuste: StateFlow<AjusteTema> get() = estado
    private val estado = MutableStateFlow(inicial)

    override fun ajusteSalvo(): AjusteTema = estado.value

    override fun salvar(ajuste: AjusteTema) {
        estado.value = ajuste
    }
}
