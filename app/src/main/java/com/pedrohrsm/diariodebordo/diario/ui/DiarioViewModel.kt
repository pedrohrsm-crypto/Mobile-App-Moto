package com.pedrohrsm.diariodebordo.diario.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.RegistroRepositorio
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Estado da moto e do filtro. Os registros vêm à parte, paginados, com os próprios carregando, vazio e erro. */
sealed interface DiarioEstado {
    data object Carregando : DiarioEstado
    data object SemMoto : DiarioEstado
    data class Pronto(val moto: Moto, val filtro: TipoRegistro?) : DiarioEstado
    data class Erro(val detalhe: String?) : DiarioEstado
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiarioViewModel @Inject constructor(motoAtual: MotoAtual, repositorio: RegistroRepositorio) : ViewModel() {

    private val filtro = MutableStateFlow<TipoRegistro?>(null)

    private val motoEFiltro = combine(motoAtual.moto, filtro) { moto, tipo -> moto to tipo }

    val estado: StateFlow<DiarioEstado> = motoEFiltro
        .map<_, DiarioEstado> { (moto, tipo) ->
            if (moto == null) DiarioEstado.SemMoto else DiarioEstado.Pronto(moto, tipo)
        }
        .catch { emit(DiarioEstado.Erro(it.message)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiarioEstado.Carregando)

    val registros: Flow<PagingData<Registro>> = motoEFiltro
        .map { (moto, tipo) -> moto?.id to tipo }
        .distinctUntilChanged()
        .flatMapLatest { (motoId, tipo) ->
            if (motoId == null) flowOf(PagingData.empty()) else repositorio.paginarDaMoto(motoId, tipo)
        }
        // Falha da moto atual já vira DiarioEstado.Erro, que a tela mostra no lugar da lista; falha do Room
        // dentro da paginação chega como LoadState.Error.
        .catch { emit(PagingData.empty()) }
        .cachedIn(viewModelScope)

    fun filtrar(tipo: TipoRegistro?) {
        filtro.value = tipo
    }
}
