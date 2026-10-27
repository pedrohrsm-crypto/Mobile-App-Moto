package com.pedrohrsm.diariodebordo.rota.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.localizacao.Ponto
import com.pedrohrsm.diariodebordo.rota.data.Rota
import com.pedrohrsm.diariodebordo.rota.data.RotaRepositorio
import com.pedrohrsm.diariodebordo.rota.dominio.Polilinha
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RotaDetalheEstado {
    data object Carregando : RotaDetalheEstado
    data object NaoEncontrada : RotaDetalheEstado

    /** [pontos]: o traçado salvo, já decodificado. */
    data class Pronto(val rota: Rota, val pontos: List<Ponto>) : RotaDetalheEstado
    data class Erro(val detalhe: String?) : RotaDetalheEstado
}

@HiltViewModel
class RotaDetalheViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val rotas: RotaRepositorio,
) : ViewModel() {

    private val rotaId: String = checkNotNull(savedStateHandle[ARG_ROTA_ID])

    val estado: StateFlow<RotaDetalheEstado> = rotas.observar(rotaId)
        // Polilinha corrompida lança aqui e cai no catch: vira Erro, não um traçado vazio.
        .map {
            if (it ==
                null
            ) {
                RotaDetalheEstado.NaoEncontrada
            } else {
                RotaDetalheEstado.Pronto(it, Polilinha.decodificar(it.polilinha))
            }
        }
        .catch { emit(RotaDetalheEstado.Erro(it.message)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RotaDetalheEstado.Carregando)

    private val _excluida = MutableStateFlow(false)
    val excluida: StateFlow<Boolean> = _excluida.asStateFlow()

    private val _falha = MutableStateFlow<String?>(null)
    val falha: StateFlow<String?> = _falha.asStateFlow()

    fun excluir() {
        viewModelScope.launch {
            runCatching { rotas.excluir(rotaId) }
                .onSuccess { _excluida.value = true }
                .onFailure { _falha.value = it.message.orEmpty() }
        }
    }

    fun limparFalha() {
        _falha.value = null
    }

    companion object {
        const val ARG_ROTA_ID = "rotaId"
    }
}
