package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.garagem.dominio.EdicaoRecusadaException
import com.pedrohrsm.diariodebordo.garagem.dominio.ExclusaoRecusadaException
import com.pedrohrsm.diariodebordo.garagem.dominio.LeituraRecusadaException
import com.pedrohrsm.diariodebordo.garagem.dominio.PontoDoHodometro
import com.pedrohrsm.diariodebordo.garagem.dominio.ValidacaoEdicao
import com.pedrohrsm.diariodebordo.garagem.dominio.ValidacaoExclusao
import com.pedrohrsm.diariodebordo.garagem.dominio.ValidacaoLeitura
import com.pedrohrsm.diariodebordo.garagem.dominio.Vizinha
import com.pedrohrsm.diariodebordo.numeros.Leitor
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

    /** [kmAtual] vem do hodômetro (maior leitura ativa), não é recalculado a partir da lista. */
    data class Pronto(val moto: Moto, val leituras: List<LeituraHodometro>, val kmAtual: Int?) : MotoDetalheEstado
    data class Erro(val detalhe: String?) : MotoDetalheEstado
}

sealed interface ErroHodometro {
    data object Invalido : ErroHodometro
    data class Menor(val anteriorKm: Int) : ErroHodometro
    data class Igual(val anteriorKm: Int) : ErroHodometro
    data class Falha(val detalhe: String?) : ErroHodometro

    /** Edição fora do intervalo das vizinhas pela data: a mensagem cita as duas (ou a que existe) e as datas. */
    data class ForaDoIntervalo(
        val violada: Vizinha,
        val anterior: PontoDoHodometro?,
        val seguinte: PontoDoHodometro?,
    ) : ErroHodometro

    data object LeituraSumiu : ErroHodometro
}

/** [editando] é a leitura que está sendo corrigida; nulo é uma leitura nova ("Atualizar hodômetro"). */
data class DialogoHodometro(
    val texto: String = "",
    val erro: ErroHodometro? = null,
    val salvando: Boolean = false,
    val editando: LeituraHodometro? = null,
)

sealed interface ErroApagar {
    data object UnicaComRegistrosDeKm : ErroApagar
    data object LeituraSumiu : ErroApagar
    data class Falha(val detalhe: String?) : ErroApagar
}

data class ConfirmacaoApagar(
    val leitura: LeituraHodometro,
    val erro: ErroApagar? = null,
    val apagando: Boolean = false,
)

@HiltViewModel
class MotoDetalheViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    motos: MotoRepositorio,
    private val hodometro: HodometroRepositorio,
    motoAtual: MotoAtual,
) : ViewModel() {

    private val motoId: String = checkNotNull(savedStateHandle[ARG_MOTO_ID])

    init {
        motoAtual.selecionar(motoId)
    }

    val estado: StateFlow<MotoDetalheEstado> =
        combine(
            motos.observar(motoId),
            hodometro.observarDaMoto(motoId),
            hodometro.observarKmAtual(motoId),
        ) { moto, leituras, kmAtual ->
            if (moto == null) MotoDetalheEstado.NaoEncontrada else MotoDetalheEstado.Pronto(moto, leituras, kmAtual)
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

    fun abrirCorrecao(leitura: LeituraHodometro) {
        _dialogo.value = DialogoHodometro(texto = leitura.km.toString(), editando = leitura)
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
            runCatching {
                val editando = atual.editando
                if (editando == null) hodometro.registrar(motoId, km) else hodometro.corrigir(editando.id, km)
            }
                .onSuccess { _dialogo.value = null }
                .onFailure { falha -> _dialogo.update { it?.copy(salvando = false, erro = erroDe(falha)) } }
        }
    }

    private val _apagar = MutableStateFlow<ConfirmacaoApagar?>(null)
    val apagar: StateFlow<ConfirmacaoApagar?> = _apagar.asStateFlow()

    fun pedirApagar(leitura: LeituraHodometro) {
        _apagar.value = ConfirmacaoApagar(leitura)
    }

    fun cancelarApagar() {
        _apagar.value = null
    }

    fun confirmarApagar() {
        val atual = _apagar.value ?: return
        if (atual.apagando) return
        _apagar.value = atual.copy(apagando = true, erro = null)
        viewModelScope.launch {
            runCatching { hodometro.apagar(atual.leitura.id) }
                .onSuccess { _apagar.value = null }
                .onFailure { falha -> _apagar.update { it?.copy(apagando = false, erro = erroDeApagar(falha)) } }
        }
    }

    private fun erroDeApagar(falha: Throwable): ErroApagar =
        when ((falha as? ExclusaoRecusadaException)?.motivo) {
            ValidacaoExclusao.UnicaComRegistrosDeKm -> ErroApagar.UnicaComRegistrosDeKm
            ValidacaoExclusao.NaoEncontrada -> ErroApagar.LeituraSumiu
            else -> ErroApagar.Falha(falha.message)
        }

    private fun erroDe(falha: Throwable): ErroHodometro =
        (falha as? EdicaoRecusadaException)?.let { erroDeEdicao(it.motivo) } ?: erroDeLeitura(falha)

    private fun erroDeEdicao(motivo: ValidacaoEdicao): ErroHodometro = when (motivo) {
        is ValidacaoEdicao.ForaDoIntervalo ->
            ErroHodometro.ForaDoIntervalo(motivo.violada, motivo.anterior, motivo.seguinte)
        ValidacaoEdicao.Negativa, ValidacaoEdicao.AcimaDoLimite -> ErroHodometro.Invalido
        ValidacaoEdicao.NaoEncontrada -> ErroHodometro.LeituraSumiu
        ValidacaoEdicao.Aceita -> ErroHodometro.Falha(null)
    }

    private fun erroDeLeitura(falha: Throwable): ErroHodometro =
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
