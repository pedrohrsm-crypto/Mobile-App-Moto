package com.pedrohrsm.diariodebordo.plano.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.numeros.Leitor
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.OrigemItem
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import com.pedrohrsm.diariodebordo.plano.dominio.CampoItem
import com.pedrohrsm.diariodebordo.plano.dominio.validarItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ItemPlanoFormEstado(
    val carregando: Boolean = true,
    val editando: Boolean = false,
    val motoId: String? = null,
    val nome: String = "",
    val km: String = "",
    val dias: String = "",
    val invalidos: Set<CampoItem> = emptySet(),
    val erro: String? = null,
    val salvando: Boolean = false,
    val concluido: Boolean = false,
)

@HiltViewModel
class ItemPlanoFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val plano: PlanoRepositorio,
    private val hodometro: HodometroRepositorio,
    private val motoAtual: MotoAtual,
) : ViewModel() {

    private val itemId: String? = savedStateHandle[ARG_ITEM_ID]
    private var original: ItemDoPlano? = null

    private val _estado = MutableStateFlow(ItemPlanoFormEstado(editando = itemId != null))
    val estado: StateFlow<ItemPlanoFormEstado> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            tentar {
                val item = itemId?.let { plano.buscar(it) }
                original = item
                val motoId = item?.motoId ?: motoAtual.moto.first()?.id
                _estado.update {
                    it.copy(
                        carregando = false,
                        motoId = motoId,
                        nome = item?.nome.orEmpty(),
                        km = item?.intervaloKm?.toString().orEmpty(),
                        dias = item?.intervaloDias?.toString().orEmpty(),
                    )
                }
            }.onFailure { e -> _estado.update { it.copy(carregando = false, erro = e.message.orEmpty()) } }
        }
    }

    fun alterar(mudanca: (ItemPlanoFormEstado) -> ItemPlanoFormEstado) = _estado.update {
        mudanca(it).copy(erro = null)
    }

    fun salvar() {
        val e = _estado.value
        val motoId = e.motoId
        if (e.salvando || e.carregando || motoId == null) return
        val invalidos = validarItem(e.nome, e.km, e.dias)
        if (invalidos.isNotEmpty()) {
            _estado.update { it.copy(invalidos = invalidos) }
            return
        }
        _estado.update { it.copy(invalidos = emptySet(), salvando = true) }
        val base = original ?: ItemDoPlano(motoId = motoId, nome = "", origem = OrigemItem.USUARIO)
        val item = base.copy(
            nome = e.nome.trim(),
            intervaloKm = e.km.takeIf { it.isNotBlank() }?.let(Leitor::km),
            intervaloDias = e.dias.takeIf { it.isNotBlank() }?.trim()?.toIntOrNull(),
        )
        viewModelScope.launch {
            tentar { plano.salvar(item, hodometro.kmAtual(motoId)) }
                .onSuccess { _estado.update { it.copy(salvando = false, concluido = true) } }
                .onFailure { f -> _estado.update { it.copy(salvando = false, erro = f.message.orEmpty()) } }
        }
    }

    fun excluir() {
        val id = original?.id ?: return
        viewModelScope.launch {
            tentar { plano.excluir(id) }
                .onSuccess { _estado.update { it.copy(concluido = true) } }
                .onFailure { f -> _estado.update { it.copy(erro = f.message.orEmpty()) } }
        }
    }

    companion object {
        const val ARG_ITEM_ID = "itemId"
    }
}
