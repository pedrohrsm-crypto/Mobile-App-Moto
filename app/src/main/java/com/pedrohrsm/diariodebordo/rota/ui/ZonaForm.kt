package com.pedrohrsm.diariodebordo.rota.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.FormularioDiario
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.TelaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.core.ui.BotaoPrincipal
import com.pedrohrsm.diariodebordo.core.ui.CampoTexto
import com.pedrohrsm.diariodebordo.core.ui.EscolhaUnica
import com.pedrohrsm.diariodebordo.core.ui.EstadoCarregando
import com.pedrohrsm.diariodebordo.core.ui.EstadoVazio
import com.pedrohrsm.diariodebordo.core.ui.tentar
import com.pedrohrsm.diariodebordo.numeros.Formatador
import com.pedrohrsm.diariodebordo.rota.data.PrivacidadeDasRotas
import com.pedrohrsm.diariodebordo.rota.data.RotaRepositorio
import com.pedrohrsm.diariodebordo.rota.dominio.PontaDaRota
import com.pedrohrsm.diariodebordo.rota.dominio.RAIOS_DE_ZONA_M
import com.pedrohrsm.diariodebordo.rota.dominio.RAIO_DE_ZONA_PADRAO_M
import com.pedrohrsm.diariodebordo.rota.dominio.ZonaDePrivacidade
import com.pedrohrsm.diariodebordo.rota.dominio.pontoDaPonta
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ZonaFormEstado(
    val carregando: Boolean = true,
    /** A zona pedida não existe, ou a rota não tem traçado para dar o centro. */
    val indisponivel: Int? = null,
    val editando: Boolean = false,
    /** Zona nova: de qual ponta da rota vem o centro. */
    val ponta: PontaDaRota? = null,
    val nome: String = "",
    val raioM: Int = RAIO_DE_ZONA_PADRAO_M,
    val nomeInvalido: Boolean = false,
    val salvando: Boolean = false,
    val erro: String? = null,
    val concluido: Boolean = false,
)

/**
 * Cria a zona a partir da ponta de uma rota gravada (sem GPS) ou edita uma existente. O centro fica só aqui dentro:
 * não vai para o estado da tela nem para os argumentos da navegação.
 */
@HiltViewModel
class ZonaFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val privacidade: PrivacidadeDasRotas,
    private val rotas: RotaRepositorio,
) : ViewModel() {
    private val zonaId: String? = savedStateHandle[ARG_ZONA_ID]
    private val rotaId: String? = savedStateHandle[ARG_ROTA_ID]
    private val ponta: PontaDaRota? = savedStateHandle.get<String>(ARG_PONTA)?.let { PontaDaRota.valueOf(it) }

    private val _estado = MutableStateFlow(ZonaFormEstado())
    val estado: StateFlow<ZonaFormEstado> = _estado.asStateFlow()

    private var original: ZonaDePrivacidade? = null
    private var centro: Pair<Double, Double>? = null

    init {
        viewModelScope.launch {
            tentar { carregar() }.onFailure { e -> _estado.update { it.copy(carregando = false, erro = e.message) } }
        }
    }

    private suspend fun carregar() {
        if (zonaId != null) {
            val zona = privacidade.atual().zonas.find { it.id == zonaId }
            original = zona
            _estado.value = if (zona == null) {
                ZonaFormEstado(carregando = false, indisponivel = R.string.zona_nao_encontrada)
            } else {
                ZonaFormEstado(carregando = false, editando = true, nome = zona.nome, raioM = zona.raioM)
            }
        } else {
            val id = checkNotNull(rotaId) { "Zona nova sem rota" }
            val pontaPedida = checkNotNull(ponta) { "Zona nova sem a ponta da rota" }
            val ponto = rotas.observar(id).first()?.let { pontoDaPonta(it.polilinha, pontaPedida) }
            centro = ponto?.let { it.latitude to it.longitude }
            _estado.value = if (ponto == null) {
                ZonaFormEstado(carregando = false, indisponivel = R.string.zona_sem_tracado)
            } else {
                ZonaFormEstado(carregando = false, ponta = pontaPedida)
            }
        }
    }

    fun alterar(mudanca: (ZonaFormEstado) -> ZonaFormEstado) = _estado.update { mudanca(it).copy(nomeInvalido = false) }

    fun salvar() {
        val atual = _estado.value
        if (atual.salvando) return
        val nome = atual.nome.trim()
        if (nome.isEmpty()) {
            _estado.update { it.copy(nomeInvalido = true) }
            return
        }
        val zona = original?.copy(nome = nome, raioM = atual.raioM)
            ?: centro?.let { (lat, lon) ->
                ZonaDePrivacidade(nome = nome, latitude = lat, longitude = lon, raioM = atual.raioM)
            }
            ?: return
        executar { privacidade.salvarZona(zona) }
    }

    fun excluir() {
        val id = original?.id ?: return
        executar { privacidade.excluirZona(id) }
    }

    private fun executar(acao: suspend () -> Unit) {
        _estado.update { it.copy(salvando = true, erro = null) }
        viewModelScope.launch {
            tentar { acao() }
                .onSuccess { _estado.update { it.copy(salvando = false, concluido = true) } }
                .onFailure { e -> _estado.update { it.copy(salvando = false, erro = e.message.orEmpty()) } }
        }
    }

    companion object {
        const val ARG_ZONA_ID = "zonaId"
        const val ARG_ROTA_ID = "rotaId"
        const val ARG_PONTA = "ponta"
    }
}

@Composable
fun ZonaFormRoute(onConcluido: () -> Unit, viewModel: ZonaFormViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.concluido) { if (estado.concluido) onConcluido() }
    ZonaFormScreen(estado, viewModel::alterar, viewModel::salvar, viewModel::excluir)
}

@Composable
fun ZonaFormScreen(
    estado: ZonaFormEstado,
    onAlterar: ((ZonaFormEstado) -> ZonaFormEstado) -> Unit = {},
    onSalvar: () -> Unit = {},
    onExcluir: () -> Unit = {},
) {
    var confirmarExclusao by remember { mutableStateOf(false) }
    TelaDiario(
        titulo = stringResource(if (estado.editando) R.string.zona_editar else R.string.zona_nova),
        acoes = {
            if (estado.editando) {
                TextButton(onClick = { confirmarExclusao = true }) { Text(stringResource(R.string.excluir)) }
            }
        },
    ) { padding ->
        val indisponivel = estado.indisponivel
        when {
            estado.carregando -> EstadoCarregando(Modifier.fillMaxSize().padding(padding))
            indisponivel != null -> EstadoVazio(stringResource(indisponivel), Modifier.fillMaxSize().padding(padding))
            else -> FormularioDiario(padding) {
                Column(verticalArrangement = Arrangement.spacedBy(LocalEscalaDiario.current.espaco(8.dp))) {
                    CampoTexto(
                        valor = estado.nome,
                        onValor = { v -> onAlterar { it.copy(nome = v) } },
                        rotulo = stringResource(R.string.zona_nome),
                        erro = stringResource(R.string.campo_obrigatorio).takeIf { estado.nomeInvalido },
                    )
                    Text(stringResource(R.string.zona_raio_grupo), style = MaterialTheme.typography.titleMedium)
                    EscolhaUnica(
                        opcoes = RAIOS_DE_ZONA_M,
                        selecionada = estado.raioM,
                        rotulo = { Formatador.distancia(it.toLong()) },
                        onEscolher = { raio -> if (raio != null) onAlterar { it.copy(raioM = raio) } },
                        permiteNenhuma = false,
                    )
                    estado.ponta?.let {
                        val texto = when (it) {
                            PontaDaRota.INICIO -> R.string.zona_centro_inicio
                            PontaDaRota.FIM -> R.string.zona_centro_fim
                        }
                        Text(stringResource(texto), style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(stringResource(R.string.zonas_explicacao), style = MaterialTheme.typography.bodySmall)
                    estado.erro?.let {
                        Text(stringResource(R.string.erro_ao_salvar, it), color = MaterialTheme.colorScheme.error)
                    }
                    BotaoPrincipal(stringResource(R.string.salvar), onSalvar, habilitado = !estado.salvando)
                }
            }
        }
    }
    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text(stringResource(R.string.zona_excluir_titulo)) },
            text = { Text(stringResource(R.string.zona_excluir_texto)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarExclusao = false
                    onExcluir()
                }) { Text(stringResource(R.string.excluir)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusao = false }) { Text(stringResource(R.string.cancelar)) }
            },
        )
    }
}
