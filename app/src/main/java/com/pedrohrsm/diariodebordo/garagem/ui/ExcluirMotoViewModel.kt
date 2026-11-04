package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.garagem.data.ExclusaoDeMoto
import com.pedrohrsm.diariodebordo.garagem.dominio.ContagemDaMoto
import com.pedrohrsm.diariodebordo.garagem.dominio.GravacaoEmAndamentoException
import com.pedrohrsm.diariodebordo.garagem.dominio.confirmaExclusao
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

/** Diálogo "Excluir moto" da tela da moto (#65). [excluida] vira true depois do commit: a tela volta para a garagem. */
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

    fun abrir(apelido: String) {
        _dialogo.value = DialogoExclusao(apelido)
        viewModelScope.launch {
            runCatching { exclusao.contar(motoId) }
                .onSuccess { contagem -> _dialogo.update { it?.copy(contagem = contagem) } }
                .onFailure { falha -> _dialogo.update { it?.copy(erro = ErroExclusao.Falha(falha.message)) } }
        }
    }

    fun fechar() {
        _dialogo.value = null
    }

    fun alterarTexto(texto: String) = _dialogo.update { it?.copy(texto = texto) }

    fun excluir() {
        val atual = _dialogo.value ?: return
        if (!atual.podeExcluir) return
        _dialogo.value = atual.copy(excluindo = true, erro = null)
        viewModelScope.launch {
            runCatching { exclusao.excluir(motoId) }
                .onSuccess {
                    _dialogo.value = null
                    _excluida.value = true
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
