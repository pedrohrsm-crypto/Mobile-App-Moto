package com.pedrohrsm.diariodebordo.plano.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.core.ui.tentar
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.numeros.Leitor
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.OrigemItem
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import com.pedrohrsm.diariodebordo.plano.data.pendencia
import com.pedrohrsm.diariodebordo.plano.dominio.CampoItem
import com.pedrohrsm.diariodebordo.plano.dominio.FeitoRecusadoException
import com.pedrohrsm.diariodebordo.plano.dominio.validarFeito
import com.pedrohrsm.diariodebordo.plano.dominio.validarItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
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
    /** O "feito" editável (#69): só na edição de um item periódico; [feitoDia] null esconde a seção. */
    val feitoKm: String = "",
    val feitoDia: LocalDate? = null,
    /** O km atual da moto, para a mensagem "entre 0 e X" do km do feito; null sem leitura. */
    val kmAtual: Int? = null,
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
    private val relogio: Clock,
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
                val kmAtual = motoId?.let { hodometro.kmAtual(it) }
                val feito = item?.takeIf { !it.pendencia }
                _estado.update {
                    it.copy(
                        carregando = false,
                        motoId = motoId,
                        nome = item?.nome.orEmpty(),
                        km = item?.intervaloKm?.toString().orEmpty(),
                        dias = item?.intervaloDias?.toString().orEmpty(),
                        feitoKm = feito?.ultimaVezKm?.toString().orEmpty(),
                        feitoDia = feito?.let { f -> dia(f.ultimaVezEm ?: f.criadoEm) },
                        kmAtual = kmAtual,
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
        val base = original ?: ItemDoPlano(motoId = motoId, nome = "", origem = OrigemItem.USUARIO)
        val feito = feitoEditado(base, e)
        val invalidos = validarItem(e.nome, e.km, e.dias) +
            feito?.let { validarFeito(e.feitoKm, it, e.kmAtual, dia(relogio.millis())) }.orEmpty()
        if (invalidos.isNotEmpty()) {
            _estado.update { it.copy(invalidos = invalidos) }
            return
        }
        _estado.update { it.copy(invalidos = emptySet(), salvando = true) }
        val item = base.copy(
            nome = e.nome.trim(),
            intervaloKm = e.km.takeIf { it.isNotBlank() }?.let(Leitor::km),
            intervaloDias = e.dias.takeIf { it.isNotBlank() }?.trim()?.toIntOrNull(),
        ).let { editado ->
            feito?.let { dia -> editado.copy(ultimaVezKm = Leitor.km(e.feitoKm), ultimaVezEm = instante(editado, dia)) }
                ?: editado
        }
        viewModelScope.launch {
            tentar { plano.salvar(item, hodometro.kmAtual(motoId)) }
                .onSuccess { _estado.update { it.copy(salvando = false, concluido = true) } }
                .onFailure { f ->
                    _estado.update {
                        // O km atual pode ter mudado desde a abertura: a recusa do repositório volta como campo.
                        if (f is FeitoRecusadoException) {
                            it.copy(salvando = false, invalidos = f.campos)
                        } else {
                            it.copy(salvando = false, erro = f.message.orEmpty())
                        }
                    }
                }
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

    /** O dia do feito, se a pessoa mudou o km ou a data; null quando o feito ficou como estava (ou não se aplica). */
    private fun feitoEditado(base: ItemDoPlano, e: ItemPlanoFormEstado): LocalDate? {
        val dia = e.feitoDia ?: return null
        val diaOriginal = dia(base.ultimaVezEm ?: base.criadoEm)
        val mudou = Leitor.km(e.feitoKm) != base.ultimaVezKm || dia != diaOriginal
        return dia.takeIf { mudou }
    }

    /** O dia escolhido com a hora da última vez de antes: só o dia muda, no fuso do relógio do app. */
    private fun instante(base: ItemDoPlano, dia: LocalDate): Long {
        val antes = Instant.ofEpochMilli(base.ultimaVezEm ?: base.criadoEm).atZone(relogio.zone)
        return dia.atTime(antes.toLocalTime()).atZone(relogio.zone).toInstant().toEpochMilli()
    }

    private fun dia(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(relogio.zone).toLocalDate()

    companion object {
        const val ARG_ITEM_ID = "itemId"
    }
}
