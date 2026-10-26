package com.pedrohrsm.diariodebordo.diario.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.RegistroRepositorio
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

sealed interface DiarioEstado {
    data object Carregando : DiarioEstado
    data object SemMoto : DiarioEstado
    data class Pronto(val moto: Moto, val registros: List<Registro>, val filtro: TipoRegistro?) : DiarioEstado
    data class Erro(val detalhe: String?) : DiarioEstado
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiarioViewModel @Inject constructor(motoAtual: MotoAtual, repositorio: RegistroRepositorio) : ViewModel() {

    private val filtro = MutableStateFlow<TipoRegistro?>(null)

    val estado: StateFlow<DiarioEstado> = motoAtual.moto
        .flatMapLatest { moto ->
            if (moto == null) {
                flowOf(DiarioEstado.SemMoto)
            } else {
                combine(repositorio.observarDaMoto(moto.id), filtro) { registros, tipo ->
                    DiarioEstado.Pronto(moto, registros.filter { tipo == null || it.tipo == tipo }, tipo)
                }
            }
        }
        .catch { emit(DiarioEstado.Erro(it.message)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiarioEstado.Carregando)

    fun filtrar(tipo: TipoRegistro?) {
        filtro.value = tipo
    }
}
