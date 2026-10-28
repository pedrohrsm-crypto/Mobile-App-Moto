package com.pedrohrsm.diariodebordo.plano.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.core.preferencias.Preferencias
import com.pedrohrsm.diariodebordo.garagem.ui.MotoDetalheViewModel
import com.pedrohrsm.diariodebordo.plano.data.AtalhoTrocaDeOleo
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import com.pedrohrsm.diariodebordo.plano.dominio.itemDeTrocaDeOleo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface EstadoAtalhoOleo {
    /** Modo oficina desligado, ou a moto não tem o item de troca de óleo no plano. */
    data object Oculto : EstadoAtalhoOleo
    data object Disponivel : EstadoAtalhoOleo
    data class Erro(val detalhe: String?) : EstadoAtalhoOleo
}

/** Confirmação aberta, gravação em curso e o resultado da última troca registrada pelo atalho. */
data class AcaoAtalhoOleo(
    val confirmando: Boolean = false,
    val gravando: Boolean = false,
    val registrado: Boolean = false,
    val falha: String? = null,
)

/** Toque 1: abrir a moto. Toque 2: "registrar troca de óleo". Toque 3: confirmar. */
@HiltViewModel
class AtalhoTrocaDeOleoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    preferencias: Preferencias,
    plano: PlanoRepositorio,
    private val atalho: AtalhoTrocaDeOleo,
) : ViewModel() {

    private val motoId: String = checkNotNull(savedStateHandle[MotoDetalheViewModel.ARG_MOTO_ID])

    val estado: StateFlow<EstadoAtalhoOleo> =
        combine(preferencias.ajuste, plano.observarDaMoto(motoId)) { ajuste, itens ->
            if (ajuste.modoOficina && itemDeTrocaDeOleo(itens) != null) {
                EstadoAtalhoOleo.Disponivel
            } else {
                EstadoAtalhoOleo.Oculto
            }
        }
            .catch { emit(EstadoAtalhoOleo.Erro(it.message)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoAtalhoOleo.Oculto)

    private val _acao = MutableStateFlow(AcaoAtalhoOleo())
    val acao: StateFlow<AcaoAtalhoOleo> = _acao.asStateFlow()

    fun pedir() = _acao.update { AcaoAtalhoOleo(confirmando = true) }

    fun cancelar() = _acao.update { it.copy(confirmando = false) }

    fun confirmar() {
        if (_acao.value.gravando) return
        _acao.value = AcaoAtalhoOleo(gravando = true)
        viewModelScope.launch {
            _acao.value = tentar { atalho.registrar(motoId) }.fold(
                onSuccess = { AcaoAtalhoOleo(registrado = true) },
                onFailure = { AcaoAtalhoOleo(falha = it.message.orEmpty()) },
            )
        }
    }
}
