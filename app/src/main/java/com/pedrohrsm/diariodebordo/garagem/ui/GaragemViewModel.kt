package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GaragemViewModel @Inject constructor(
    repositorio: MotoRepositorio,
) : ViewModel() {

    val estado: StateFlow<GaragemEstado> = repositorio.observarAtivos()
        .map<_, GaragemEstado> { GaragemEstado.Pronto(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GaragemEstado.Carregando)
}
