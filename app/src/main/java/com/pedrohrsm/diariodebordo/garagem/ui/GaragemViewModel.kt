package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.garagem.dominio.podeCadastrarMoto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class GaragemViewModel @Inject constructor(repositorio: MotoRepositorio, hodometro: HodometroRepositorio) :
    ViewModel() {

    val estado: StateFlow<GaragemEstado> =
        combine(repositorio.observarAtivos(), hodometro.observarKmAtuais()) { motos, kms ->
            GaragemEstado.Pronto(
                motos = motos,
                kmPorMoto = kms.associate { it.motoId to it.km },
                podeAdicionar = podeCadastrarMoto(motos.size),
            ) as GaragemEstado
        }
            .catch { emit(GaragemEstado.Erro(it.message)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GaragemEstado.Carregando)
}
