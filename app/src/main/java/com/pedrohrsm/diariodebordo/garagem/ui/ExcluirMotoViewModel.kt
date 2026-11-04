package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.garagem.data.ExclusaoDeMoto
import com.pedrohrsm.diariodebordo.garagem.dominio.ContagemDaMoto
import com.pedrohrsm.diariodebordo.garagem.dominio.GravacaoEmAndamentoException
import com.pedrohrsm.diariodebordo.garagem.dominio.confirmaExclusao
import com.pedrohrsm.diariodebordo.plano.ui.tentar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ErroExclusao {
    data object Gravando : ErroExclusao
    data class Falha(val detalhe: String?) : ErroExclusao
}

/** [contagem] null enquanto conta; [apelido] é o que a pessoa precisa digitar para liberar o botão. */
data class DialogoExclusao(
    val apelido: String,
    val contagem: ContagemDaMoto? = null,
    val texto: String = "",
    val excluindo: Boolean = false,
    val erro: ErroExclusao? = null,
) {
    val podeExcluir: Boolean get() = contagem != null && !excluindo && confirmaExclusao(texto, apelido)
}

/**
 * A moto foi excluída, mas um passo depois do commit falhou (#72): [fotosQueFicaram] arquivos não saíram do
 * armazenamento, ou os lembretes não foram recalculados agora (a verificação diária os confere de novo).
 */
data class AvisoExclusao(val fotosQueFicaram: Int, val lembretesRecalculados: Boolean)

/**
 * Diálogo "Excluir moto" da tela da moto (#65). [excluida] vira true depois do commit: a tela volta para a garagem.
 * Se um passo depois do commit falhou, antes aparece o [aviso]; ao fechá-lo, a tela volta para a garagem.
 */
@HiltViewModel
class ExcluirMotoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exclusao: ExclusaoDeMoto,
) : ViewModel() {

    private val motoId: String = checkNotNull(savedStateHandle[MotoDetalheViewModel.ARG_MOTO_ID])

    private val _dialogo = MutableStateFlow<DialogoExclusao?>(null)
    val dialogo: StateFlow<DialogoExclusao?> = _dialogo.asStateFlow()

    private val _excluida = MutableStateFlow(false)
    val excluida: StateFlow<Boolean> = _excluida.asStateFlow()

    private val _aviso = MutableStateFlow<AvisoExclusao?>(null)
    val aviso: StateFlow<AvisoExclusao?> = _aviso.asStateFlow()

    fun abrir(apelido: String) {
        _dialogo.value = DialogoExclusao(apelido)
        viewModelScope.launch {
            tentar { exclusao.contar(motoId) }
                .onSuccess { contagem -> _dialogo.update { it?.copy(contagem = contagem) } }
                .onFailure { falha -> _dialogo.update { it?.copy(erro = ErroExclusao.Falha(falha.message)) } }
        }
    }

    fun fechar() {
        _dialogo.value = null
    }

    /** A pessoa leu o aviso: a moto já foi excluída, a tela volta para a garagem. */
    fun fecharAviso() {
        if (_aviso.value == null) return
        _aviso.value = null
        _excluida.value = true
    }

    fun alterarTexto(texto: String) = _dialogo.update { it?.copy(texto = texto) }

    fun excluir() {
        val atual = _dialogo.value ?: return
        if (!atual.podeExcluir) return
        _dialogo.value = atual.copy(excluindo = true, erro = null)
        viewModelScope.launch {
            tentar { exclusao.excluir(motoId) }
                .onSuccess { resultado ->
                    _dialogo.value = null
                    if (resultado.fotosQueFicaram.isEmpty() && resultado.lembretesRecalculados) {
                        _excluida.value = true
                    } else {
                        _aviso.value = AvisoExclusao(resultado.fotosQueFicaram.size, resultado.lembretesRecalculados)
                    }
                }
                .onFailure { falha ->
                    val erro = if (falha is GravacaoEmAndamentoException) {
                        ErroExclusao.Gravando
                    } else {
                        ErroExclusao.Falha(falha.message)
                    }
                    _dialogo.update { it?.copy(excluindo = false, erro = erro) }
                }
        }
    }
}
