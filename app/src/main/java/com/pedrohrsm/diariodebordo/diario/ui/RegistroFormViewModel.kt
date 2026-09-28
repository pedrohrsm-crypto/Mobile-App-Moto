package com.pedrohrsm.diariodebordo.diario.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.RegistroRepositorio
import com.pedrohrsm.diariodebordo.diario.data.TipoOcorrencia
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.diario.dominio.CampoRegistro
import com.pedrohrsm.diariodebordo.diario.dominio.MAXIMO_FOTOS
import com.pedrohrsm.diariodebordo.diario.dominio.validarRegistro
import com.pedrohrsm.diariodebordo.diario.fotos.GuardaFotos
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.numeros.Leitor
import com.pedrohrsm.diariodebordo.numeros.TextoDeNumero
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ErroRegistroForm {
    data object SemMoto : ErroRegistroForm
    data object NaoEncontrado : ErroRegistroForm
    data class FalhaFoto(val detalhe: String?) : ErroRegistroForm
    data class Falha(val detalhe: String?) : ErroRegistroForm
}

data class RegistroFormEstado(
    val carregando: Boolean = true,
    val editando: Boolean = false,
    val motoId: String? = null,
    val tipo: TipoRegistro = TipoRegistro.MANUTENCAO,
    val titulo: String = "",
    val descricao: String = "",
    val dataEm: Long = 0,
    val km: String = "",
    val valor: String = "",
    val litros: String = "",
    val tanqueCheio: Boolean = false,
    val ocorrencia: TipoOcorrencia? = null,
    val itemPlanoId: String? = null,
    val rotaId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val fotos: List<String> = emptyList(),
    val importandoFoto: Boolean = false,
    val invalidos: Set<CampoRegistro> = emptySet(),
    val erro: ErroRegistroForm? = null,
    val salvando: Boolean = false,
    val concluido: Boolean = false,
) {
    val podeAdicionarFoto: Boolean get() = fotos.size < MAXIMO_FOTOS && !importandoFoto
}

@HiltViewModel
class RegistroFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repositorio: RegistroRepositorio,
    private val guardaFotos: GuardaFotos,
    private val motoAtual: MotoAtual,
    private val plano: PlanoRepositorio,
    private val hodometro: HodometroRepositorio,
    private val relogio: Clock,
) : ViewModel() {

    private val registroId: String? = savedStateHandle[ARG_REGISTRO_ID]
    private var original: Registro? = null

    /** Fotos importadas nesta edição: se a pessoa sair sem salvar, os arquivos são apagados. */
    private val fotosNovas = mutableSetOf<String>()

    private val _estado = MutableStateFlow(RegistroFormEstado(editando = registroId != null))
    val estado: StateFlow<RegistroFormEstado> = _estado.asStateFlow()

    init {
        val tipoInicial = savedStateHandle.get<String>(ARG_TIPO)
            ?.let { nome -> TipoRegistro.entries.find { it.name == nome } }
        val itemInicial: String? = savedStateHandle[ARG_ITEM_ID]
        viewModelScope.launch {
            runCatching { carregar(tipoInicial, itemInicial) }
                .onFailure { e ->
                    _estado.update { it.copy(carregando = false, erro = ErroRegistroForm.Falha(e.message)) }
                }
        }
    }

    private suspend fun carregar(tipoInicial: TipoRegistro?, itemInicial: String?) {
        if (registroId == null) {
            val moto = motoAtual.moto.first()
            // Vindo do plano ("registrar feito"): título do item e km atual já preenchidos, só falta salvar.
            val item = itemInicial?.let { plano.buscar(it) }
            val kmAtual = if (item != null) hodometro.kmAtual(item.motoId) else null
            _estado.update {
                it.copy(
                    carregando = false,
                    motoId = item?.motoId ?: moto?.id,
                    erro = if (moto == null && item == null) ErroRegistroForm.SemMoto else null,
                    tipo = if (item != null) TipoRegistro.MANUTENCAO else tipoInicial ?: it.tipo,
                    titulo = item?.nome.orEmpty(),
                    km = kmAtual?.toString().orEmpty(),
                    itemPlanoId = item?.id,
                    dataEm = relogio.millis(),
                )
            }
            return
        }
        val registro = repositorio.buscar(registroId)
        if (registro == null) {
            _estado.update { it.copy(carregando = false, erro = ErroRegistroForm.NaoEncontrado) }
            return
        }
        original = registro
        val fotos = repositorio.fotosDo(registro.id).map { it.arquivo }
        _estado.update { estadoDe(registro, fotos) }
    }

    fun alterar(mudanca: (RegistroFormEstado) -> RegistroFormEstado) = _estado.update { mudanca(it).copy(erro = null) }

    fun adicionarFoto(uri: String) {
        if (!_estado.value.podeAdicionarFoto) return
        _estado.update { it.copy(importandoFoto = true, erro = null) }
        viewModelScope.launch {
            runCatching { guardaFotos.importar(uri) }
                .onSuccess { arquivo ->
                    fotosNovas += arquivo
                    _estado.update { it.copy(importandoFoto = false, fotos = it.fotos + arquivo) }
                }
                .onFailure { e ->
                    _estado.update { it.copy(importandoFoto = false, erro = ErroRegistroForm.FalhaFoto(e.message)) }
                }
        }
    }

    fun removerFoto(arquivo: String) {
        _estado.update { it.copy(fotos = it.fotos - arquivo) }
        // Foto que ainda não foi salva sai do armazenamento na hora; a já salva só depois de salvar.
        if (fotosNovas.remove(arquivo)) guardaFotos.apagar(arquivo)
    }

    fun salvar() {
        val e = _estado.value
        val motoId = e.motoId
        if (e.salvando || e.carregando || motoId == null) return
        val invalidos = validarRegistro(e.tipo, e.titulo, e.km, e.valor, e.litros, e.fotos.size)
        if (invalidos.isNotEmpty()) {
            _estado.update { it.copy(invalidos = invalidos) }
            return
        }
        _estado.update { it.copy(invalidos = emptySet(), salvando = true, erro = null) }
        val registro = registroDe(e, motoId)
        viewModelScope.launch {
            runCatching { repositorio.salvar(registro, e.fotos) }
                .onSuccess { salvo ->
                    fotosNovas.clear()
                    salvo.arquivosRemovidos.forEach { guardaFotos.apagar(it) }
                    _estado.update { it.copy(salvando = false, concluido = true) }
                }
                .onFailure { falha ->
                    _estado.update { it.copy(salvando = false, erro = ErroRegistroForm.Falha(falha.message)) }
                }
        }
    }

    fun excluir() {
        val id = original?.id ?: return
        _estado.update { it.copy(salvando = true, erro = null) }
        viewModelScope.launch {
            runCatching { repositorio.excluir(id) }
                .onSuccess { arquivos ->
                    arquivos.forEach { guardaFotos.apagar(it) }
                    _estado.update { it.copy(salvando = false, concluido = true) }
                }
                .onFailure { falha ->
                    _estado.update { it.copy(salvando = false, erro = ErroRegistroForm.Falha(falha.message)) }
                }
        }
    }

    override fun onCleared() = descartarFotosNaoSalvas()

    /** Apaga as fotos importadas que não chegaram a ser salvas (a pessoa saiu sem salvar). */
    fun descartarFotosNaoSalvas() {
        if (_estado.value.concluido) return
        fotosNovas.forEach { guardaFotos.apagar(it) }
        fotosNovas.clear()
    }

    private fun registroDe(e: RegistroFormEstado, motoId: String): Registro {
        val abastecimento = e.tipo == TipoRegistro.ABASTECIMENTO
        val base = original ?: Registro(motoId = motoId, tipo = e.tipo, titulo = "", dataEm = e.dataEm)
        return base.copy(
            tipo = e.tipo,
            titulo = e.titulo.trim(),
            descricao = e.descricao.trim(),
            dataEm = e.dataEm,
            hodometroKm = e.km.takeIf { it.isNotBlank() }?.let(Leitor::km),
            valorCentavos = e.valor.takeIf { it.isNotBlank() }?.let(Leitor::dinheiro),
            litrosMl = e.litros.takeIf { it.isNotBlank() && abastecimento }?.let(Leitor::litros),
            tanqueCheio = abastecimento && e.tanqueCheio,
            ocorrencia = e.ocorrencia.takeIf { e.tipo == TipoRegistro.OCORRENCIA },
            itemPlanoId = e.itemPlanoId.takeIf { e.tipo == TipoRegistro.MANUTENCAO },
            rotaId = e.rotaId,
            latitude = e.latitude,
            longitude = e.longitude,
        )
    }

    private fun estadoDe(r: Registro, fotos: List<String>) = RegistroFormEstado(
        carregando = false,
        editando = true,
        motoId = r.motoId,
        tipo = r.tipo,
        titulo = r.titulo,
        descricao = r.descricao,
        dataEm = r.dataEm,
        km = r.hodometroKm?.toString().orEmpty(),
        valor = r.valorCentavos?.let { TextoDeNumero.dinheiro(it) }.orEmpty(),
        litros = r.litrosMl?.let { TextoDeNumero.litros(it) }.orEmpty(),
        tanqueCheio = r.tanqueCheio,
        ocorrencia = r.ocorrencia,
        itemPlanoId = r.itemPlanoId,
        rotaId = r.rotaId,
        latitude = r.latitude,
        longitude = r.longitude,
        fotos = fotos,
    )

    companion object {
        const val ARG_REGISTRO_ID = "registroId"
        const val ARG_TIPO = "tipo"
        const val ARG_ITEM_ID = "itemId"
    }
}
