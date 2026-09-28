package com.pedrohrsm.diariodebordo.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.core.preferencias.Preferencias
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AjustesEstado(val tema: TemaDiario = TemaDiario.OFICINA, val modoOficina: Boolean = false)

@HiltViewModel
class AjustesViewModel @Inject constructor(private val preferencias: Preferencias) : ViewModel() {

    val estado: StateFlow<AjustesEstado> = combine(preferencias.tema, preferencias.modoOficina, ::AjustesEstado)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AjustesEstado())

    fun escolherTema(tema: TemaDiario) = preferencias.definirTema(tema)

    fun alternarModoOficina(ligado: Boolean) = preferencias.definirModoOficina(ligado)
}
