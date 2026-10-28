package com.pedrohrsm.diariodebordo.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import com.pedrohrsm.diariodebordo.core.preferencias.Preferencias
import com.pedrohrsm.diariodebordo.core.premium.Premium
import com.pedrohrsm.diariodebordo.core.premium.RecursoPremium
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AjustesViewModel @Inject constructor(private val preferencias: Preferencias, premium: Premium) : ViewModel() {

    val ajuste: StateFlow<AjusteTema> = preferencias.ajuste
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), preferencias.ajusteSalvo())

    /** Vazio com a flag desligada: a tela não mostra nada do premium. */
    val recursosPremium: Set<RecursoPremium> = premium.recursosVisiveis()

    /** Grava e a MainActivity, que observa as preferências, troca o tema na hora. */
    fun ajustar(ajuste: AjusteTema) = preferencias.salvar(ajuste)
}
