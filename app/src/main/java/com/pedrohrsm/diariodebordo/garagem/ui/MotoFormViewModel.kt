package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.core.premium.LimiteDeMotosException
import com.pedrohrsm.diariodebordo.garagem.data.CadastroMoto
import com.pedrohrsm.diariodebordo.garagem.data.CategoriaMoto
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.garagem.dominio.CampoMoto
import com.pedrohrsm.diariodebordo.garagem.dominio.validarMoto
import com.pedrohrsm.diariodebordo.numeros.Leitor
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Year
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ErroMotoForm {
    data object NaoEncontrada : ErroMotoForm
    data class LimiteGratis(val limite: Int) : ErroMotoForm
    data class Falha(val detalhe: String?) : ErroMotoForm
}

data class MotoFormEstado(
    val editando: Boolean = false,
    val carregando: Boolean = false,
    val apelido: String = "",
    val marca: String = "",
    val modelo: String = "",
    val ano: String = "",
    val categoria: CategoriaMoto? = null,
    val kmInicial: String = "",
    val invalidos: Set<CampoMoto> = emptySet(),
    val erro: ErroMotoForm? = null,
    val salvando: Boolean = false,
    val salvoId: String? = null,
)

@HiltViewModel
class MotoFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val motos: MotoRepositorio,
    private val cadastro: CadastroMoto,
    private val motoAtual: MotoAtual,
    private val relogio: Clock,
) : ViewModel() {

    private val motoId: String? = savedStateHandle[ARG_MOTO_ID]
    private var original: Moto? = null

    private val _estado = MutableStateFlow(MotoFormEstado(editando = motoId != null, carregando = motoId != null))
    val estado: StateFlow<MotoFormEstado> = _estado.asStateFlow()

    init {
        if (motoId != null) {
            viewModelScope.launch {
                val moto = runCatching { motos.observar(motoId).first() }
                    .onFailure { e ->
                        _estado.update { it.copy(carregando = false, erro = ErroMotoForm.Falha(e.message)) }
                    }
                    .getOrNull() ?: run {
                    _estado.update { it.copy(carregando = false, erro = it.erro ?: ErroMotoForm.NaoEncontrada) }
                    return@launch
                }
                original = moto
                _estado.update {
                    it.copy(
                        carregando = false,
                        apelido = moto.apelido,
                        marca = moto.marca,
                        modelo = moto.modelo,
                        ano = moto.ano?.toString().orEmpty(),
                        categoria = moto.categoria,
                    )
                }
            }
        }
    }

    fun alterar(mudanca: (MotoFormEstado) -> MotoFormEstado) = _estado.update { mudanca(it).copy(erro = null) }

    fun salvar() {
        val e = _estado.value
        if (e.salvando || e.carregando) return
        val invalidos = validarMoto(
            apelido = e.apelido,
            marca = e.marca,
            modelo = e.modelo,
            anoTexto = e.ano,
            kmTexto = if (e.editando) null else e.kmInicial,
            anoAtual = Year.now(relogio).value,
        )
        if (invalidos.isNotEmpty()) {
            _estado.update { it.copy(invalidos = invalidos) }
            return
        }
        _estado.update { it.copy(invalidos = emptySet(), salvando = true, erro = null) }
        viewModelScope.launch {
            val dados = { base: Moto ->
                base.copy(
                    apelido = e.apelido.trim(),
                    marca = e.marca.trim(),
                    modelo = e.modelo.trim(),
                    ano = e.ano.trim().toIntOrNull(),
                    categoria = e.categoria,
                )
            }
            runCatching {
                val base = original
                if (base != null) {
                    motos.atualizar(dados(base))
                } else {
                    val km = checkNotNull(Leitor.km(e.kmInicial))
                    cadastro.cadastrar(dados(Moto(apelido = "", marca = "", modelo = "")), km)
                        .also { motoAtual.selecionar(it.id) }
                }
            }.onSuccess { salva ->
                _estado.update { it.copy(salvando = false, salvoId = salva.id) }
            }.onFailure { falha ->
                val erro = when (falha) {
                    is LimiteDeMotosException -> ErroMotoForm.LimiteGratis(falha.limite)
                    else -> ErroMotoForm.Falha(falha.message)
                }
                _estado.update { it.copy(salvando = false, erro = erro) }
            }
        }
    }

    companion object {
        const val ARG_MOTO_ID = "motoId"
    }
}
