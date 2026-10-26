package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.garagem.dominio.LeituraRecusadaException
import com.pedrohrsm.diariodebordo.garagem.dominio.ValidacaoLeitura
import com.pedrohrsm.diariodebordo.numeros.Leitor
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

sealed interface MotoDetalheEstado {
    data object Carregando : MotoDetalheEstado
    data object NaoEncontrada : MotoDetalheEstado

    /** [trocaDeOleoId]: item do plano usado pelo atalho "registrar troca de óleo"; null esconde o atalho. */
    data class Pronto(val moto: Moto, val leituras: List<LeituraHodometro>, val trocaDeOleoId: String? = null) :
        MotoDetalheEstado {
        val kmAtual: Int? get() = leituras.maxOfOrNull { it.km }
    }
    data class Erro(val detalhe: String?) : MotoDetalheEstado
}

sealed interface ErroHodometro {
    data object Invalido : ErroHodometro
    data class Menor(val anteriorKm: Int) : ErroHodometro
    data class Igual(val anteriorKm: Int) : ErroHodometro
    data class Falha(val detalhe: String?) : ErroHodometro
}

data class DialogoHodometro(val texto: String = "", val erro: ErroHodometro? = null, val salvando: Boolean = false)

@HiltViewModel
class MotoDetalheViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    motos: MotoRepositorio,
    private val hodometro: HodometroRepositorio,
    plano: PlanoRepositorio,
    motoAtual: MotoAtual,
) : ViewModel() {

    private val motoId: String = checkNotNull(savedStateHandle[ARG_MOTO_ID])

    init {
        motoAtual.selecionar(motoId)
    }

    val estado: StateFlow<MotoDetalheEstado> =
        combine(motos.observar(motoId), hodometro.observarDaMoto(motoId), plano.observarDaMoto(motoId)) {
                moto,
                leituras,
                itens,
            ->
            if (moto == null) {
                MotoDetalheEstado.NaoEncontrada
            } else {
                MotoDetalheEstado.Pronto(moto, leituras, itemDeTrocaDeOleo(itens)?.id)
            }
        }
            .catch { emit(MotoDetalheEstado.Erro(it.message)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MotoDetalheEstado.Carregando)

    private val _dialogo = MutableStateFlow<DialogoHodometro?>(null)
    val dialogo: StateFlow<DialogoHodometro?> = _dialogo.asStateFlow()

    fun abrirHodometro() {
        _dialogo.value = DialogoHodometro()
    }

    fun fecharHodometro() {
        _dialogo.value = null
    }

    fun alterarHodometro(texto: String) = _dialogo.update { it?.copy(texto = texto, erro = null) }

    fun salvarHodometro() {
        val atual = _dialogo.value ?: return
        if (atual.salvando) return
        val km = Leitor.km(atual.texto)
        if (km == null) {
            _dialogo.value = atual.copy(erro = ErroHodometro.Invalido)
            return
        }
        _dialogo.value = atual.copy(salvando = true, erro = null)
        viewModelScope.launch {
            runCatching { hodometro.registrar(motoId, km) }
                .onSuccess { _dialogo.value = null }
                .onFailure { falha -> _dialogo.update { it?.copy(salvando = false, erro = erroDe(falha)) } }
        }
    }

    private fun erroDe(falha: Throwable): ErroHodometro =
        when (val motivo = (falha as? LeituraRecusadaException)?.motivo) {
            is ValidacaoLeitura.MenorQueAnterior -> ErroHodometro.Menor(motivo.anteriorKm)
            is ValidacaoLeitura.IgualAnterior -> ErroHodometro.Igual(motivo.anteriorKm)
            ValidacaoLeitura.Negativa, ValidacaoLeitura.AcimaDoLimite -> ErroHodometro.Invalido
            else -> ErroHodometro.Falha(falha.message)
        }

    companion object {
        const val ARG_MOTO_ID = "motoId"
    }
}
