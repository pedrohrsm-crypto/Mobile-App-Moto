package com.pedrohrsm.diariodebordo.rota.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.rota.data.Rota
import com.pedrohrsm.diariodebordo.rota.data.RotaRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Gravação aberta no banco. [capturando] = o serviço está ativo; falso = foi interrompida. */
data class Gravacao(val rota: Rota, val pontos: Int, val capturando: Boolean)

sealed interface RotasEstado {
    data object Carregando : RotasEstado
    data object SemMoto : RotasEstado
    data class Pronto(val moto: Moto, val gravacao: Gravacao?, val rotas: List<Rota>) : RotasEstado
    data class Erro(val detalhe: String?) : RotasEstado
}

/** Por que a gravação não andou. O texto mostrado vem de strings.xml; o detalhe é só a mensagem técnica. */
sealed interface FalhaRota {
    data object SemPermissao : FalhaRota
    data object SemNotificacao : FalhaRota
    data class Gps(val detalhe: String?) : FalhaRota
    data class AoSalvar(val detalhe: String?) : FalhaRota
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RotasViewModel @Inject constructor(
    motoAtual: MotoAtual,
    private val rotas: RotaRepositorio,
    private val captura: StatusCaptura,
) : ViewModel() {

    private val gravacao: Flow<Gravacao?> = rotas.observarEmGravacao().flatMapLatest { rota ->
        if (rota == null) {
            flowOf(null)
        } else {
            combine(rotas.observarQuantidadeDePontos(rota.id), captura.ativo) { n, ativo ->
                Gravacao(rota, n, ativo)
            }
        }
    }

    val estado: StateFlow<RotasEstado> = motoAtual.moto
        .flatMapLatest { moto ->
            if (moto == null) {
                flowOf(RotasEstado.SemMoto)
            } else {
                combine(rotas.observarSalvasDaMoto(moto.id), gravacao) { salvas, g ->
                    RotasEstado.Pronto(moto, g, salvas)
                }
            }
        }
        .catch { emit(RotasEstado.Erro(it.message)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RotasEstado.Carregando)

    private val _falha = MutableStateFlow<FalhaRota?>(null)
    val falha: StateFlow<FalhaRota?> = combine(_falha, captura.falha) { a, b -> a ?: b }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Gravação interrompida (o sistema encerrou o app): salva com os pontos que chegaram. */
    fun salvarInterrompida(rotaId: String, tituloDoRole: String) = executar { rotas.finalizar(rotaId, tituloDoRole) }

    fun descartar(rotaId: String) = executar { rotas.descartar(rotaId) }

    /** A pessoa negou a localização: a gravação não começa, e o resto do app segue igual. */
    fun localNegado() {
        _falha.value = FalhaRota.SemPermissao
    }

    /** Android 13+: sem a notificação, o aviso fixo e o "Parar e salvar" somem; a gravação não começa. */
    fun notificacaoNegada() {
        _falha.value = FalhaRota.SemNotificacao
    }

    fun limparFalha() {
        _falha.value = null
        captura.limparFalha()
    }

    private fun executar(acao: suspend () -> Unit) {
        viewModelScope.launch { runCatching { acao() }.onFailure { _falha.value = FalhaRota.AoSalvar(it.message) } }
    }
}

/** Estado do serviço de gravação visto pelas telas. Interface para testar sem o serviço. */
interface StatusCaptura {
    val ativo: Flow<Boolean>
    val falha: Flow<FalhaRota?>
    fun limparFalha()
}
