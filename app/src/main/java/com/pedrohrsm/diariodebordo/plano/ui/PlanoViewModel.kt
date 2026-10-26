package com.pedrohrsm.diariodebordo.plano.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import com.pedrohrsm.diariodebordo.plano.data.intervalo
import com.pedrohrsm.diariodebordo.plano.data.pendencia
import com.pedrohrsm.diariodebordo.plano.dominio.SituacaoDoItem
import com.pedrohrsm.diariodebordo.plano.dominio.situacaoDoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ItemComSituacao(val item: ItemDoPlano, val situacao: SituacaoDoItem)

sealed interface PlanoEstado {
    data object Carregando : PlanoEstado
    data object SemMoto : PlanoEstado
    data class Pronto(
        val moto: Moto,
        val kmAtual: Int?,
        val pendencias: List<ItemDoPlano>,
        /** Do mais urgente para o menos urgente. */
        val itens: List<ItemComSituacao>,
    ) : PlanoEstado
    data class Erro(val detalhe: String?) : PlanoEstado
}

/** Monta a lista do plano com a situação de cada item, na ordem de urgência. */
fun montarPlano(moto: Moto, itens: List<ItemDoPlano>, kmAtual: Int?, relogio: Clock): PlanoEstado.Pronto {
    val periodicos = itens.filter { !it.pendencia }.mapNotNull { item ->
        val intervalo = item.intervalo ?: return@mapNotNull null
        val situacao = situacaoDoItem(
            intervalo = intervalo,
            ultimaVezKm = item.ultimaVezKm ?: kmAtual ?: 0,
            ultimaVezEm = item.ultimaVezEm ?: item.criadoEm,
            kmAtual = kmAtual ?: item.ultimaVezKm ?: 0,
            agora = relogio.instant(),
            fuso = relogio.zone,
        )
        ItemComSituacao(item, situacao)
    }.sortedWith(
        compareByDescending<ItemComSituacao> { it.situacao.urgencia }
            .thenBy { it.situacao.situacao.kmRestantes ?: Int.MAX_VALUE }
            .thenBy { it.situacao.situacao.diasRestantes ?: Int.MAX_VALUE },
    )
    return PlanoEstado.Pronto(moto, kmAtual, itens.filter { it.pendencia }, periodicos)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlanoViewModel @Inject constructor(
    motoAtual: MotoAtual,
    private val plano: PlanoRepositorio,
    hodometro: HodometroRepositorio,
    relogio: Clock,
) : ViewModel() {

    val estado: StateFlow<PlanoEstado> = motoAtual.moto
        .flatMapLatest { moto ->
            if (moto == null) {
                flowOf(PlanoEstado.SemMoto)
            } else {
                combine(plano.observarDaMoto(moto.id), hodometro.observarDaMoto(moto.id)) { itens, leituras ->
                    montarPlano(moto, itens, leituras.maxOfOrNull { it.km }, relogio)
                }
            }
        }
        .catch { emit(PlanoEstado.Erro(it.message)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanoEstado.Carregando)

    private val _falha = MutableStateFlow<String?>(null)
    val falha: StateFlow<String?> = _falha.asStateFlow()

    fun resolver(id: String) {
        viewModelScope.launch {
            runCatching { plano.resolverPendencia(id) }.onFailure { _falha.value = it.message ?: "" }
        }
    }

    fun limparFalha() {
        _falha.value = null
    }
}
