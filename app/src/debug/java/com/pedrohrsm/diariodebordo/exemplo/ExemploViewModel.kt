package com.pedrohrsm.diariodebordo.exemplo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EstadoExemplo {
    data object Ocioso : EstadoExemplo
    data object Carregando : EstadoExemplo
    data class Carregado(val motos: Int, val registros: Int) : EstadoExemplo
    data object Falhou : EstadoExemplo
}

@HiltViewModel
class ExemploViewModel @Inject constructor(private val carregador: CarregadorDeExemplo) : ViewModel() {
    private val _estado = MutableStateFlow<EstadoExemplo>(EstadoExemplo.Ocioso)
    val estado: StateFlow<EstadoExemplo> = _estado.asStateFlow()

    fun carregar() {
        if (_estado.value == EstadoExemplo.Carregando) return
        _estado.value = EstadoExemplo.Carregando
        viewModelScope.launch {
            _estado.value = try {
                val dados = carregador.carregar()
                EstadoExemplo.Carregado(dados.motos.size, dados.registros.size)
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                // A falha vira estado visível na tela; nada de texto da exceção em log.
                EstadoExemplo.Falhou
            }
        }
    }
}
