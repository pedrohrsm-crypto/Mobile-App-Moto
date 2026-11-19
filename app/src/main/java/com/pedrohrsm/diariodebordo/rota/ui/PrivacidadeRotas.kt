package com.pedrohrsm.diariodebordo.rota.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.TelaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.core.ui.EstadoCarregando
import com.pedrohrsm.diariodebordo.core.ui.EstadoErro
import com.pedrohrsm.diariodebordo.core.ui.tentar
import com.pedrohrsm.diariodebordo.garagem.ui.BotaoSecundario
import com.pedrohrsm.diariodebordo.numeros.Formatador
import com.pedrohrsm.diariodebordo.rota.data.PrivacidadeDasRotas
import com.pedrohrsm.diariodebordo.rota.dominio.CorteDasPontas
import com.pedrohrsm.diariodebordo.rota.dominio.ProtecaoDasRotas
import com.pedrohrsm.diariodebordo.rota.dominio.ZonaDePrivacidade
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PrivacidadeEstado {
    data object Carregando : PrivacidadeEstado
    data class Pronto(val protecao: ProtecaoDasRotas) : PrivacidadeEstado

    /** Zonas ilegíveis: a tela mostra o erro; nunca uma lista vazia, que pareceria "sem proteção". */
    data class Erro(val detalhe: String?) : PrivacidadeEstado
}

/** Corte das pontas em Ajustes e a lista de zonas (#78). */
@HiltViewModel
class PrivacidadeRotasViewModel @Inject constructor(private val privacidade: PrivacidadeDasRotas) : ViewModel() {

    val estado: StateFlow<PrivacidadeEstado> = privacidade.protecao
        .map<ProtecaoDasRotas, PrivacidadeEstado> { PrivacidadeEstado.Pronto(it) }
        .catch { emit(PrivacidadeEstado.Erro(it.message)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrivacidadeEstado.Carregando)

    private val _falha = MutableStateFlow<String?>(null)
    val falha: StateFlow<String?> = _falha.asStateFlow()

    fun escolherCorte(corte: CorteDasPontas) {
        viewModelScope.launch {
            tentar { privacidade.salvarCorte(corte) }.onFailure { _falha.value = it.message.orEmpty() }
        }
    }
}

/** Seção de Ajustes, logo depois da aparência. */
@Composable
fun SecaoPrivacidadeRotas(onZonas: () -> Unit, viewModel: PrivacidadeRotasViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val falha by viewModel.falha.collectAsStateWithLifecycle()
    SecaoPrivacidadeRotasConteudo(estado, viewModel::escolherCorte, onZonas, falha)
}

@Composable
fun SecaoPrivacidadeRotasConteudo(
    estado: PrivacidadeEstado,
    onCorte: (CorteDasPontas) -> Unit = {},
    onZonas: () -> Unit = {},
    falha: String? = null,
) {
    val escala = LocalEscalaDiario.current
    Column(verticalArrangement = Arrangement.spacedBy(escala.espaco(8.dp))) {
        Text(stringResource(R.string.privacidade_rotas_titulo), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.privacidade_rotas_explicacao),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (estado) {
            PrivacidadeEstado.Carregando -> EstadoCarregando()
            is PrivacidadeEstado.Erro -> EstadoErro(estado.detalhe)
            is PrivacidadeEstado.Pronto -> {
                Text(stringResource(R.string.privacidade_corte_grupo), style = MaterialTheme.typography.bodyLarge)
                Column(Modifier.selectableGroup()) {
                    CorteDasPontas.entries.forEach { corte ->
                        val marcado = corte == estado.protecao.corte
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = escala.alvoMinimo)
                                .selectable(selected = marcado, onClick = { onCorte(corte) }, role = Role.RadioButton),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = marcado, onClick = null)
                            Text(
                                text = rotuloDoCorte(corte),
                                modifier = Modifier.padding(start = escala.espaco(16.dp)),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
                BotaoSecundario(
                    stringResource(R.string.privacidade_zonas_abrir, estado.protecao.zonas.size),
                    onZonas,
                )
            }
        }
        falha?.let {
            Text(stringResource(R.string.erro_ao_salvar, it), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun rotuloDoCorte(corte: CorteDasPontas): String = if (corte == CorteDasPontas.DESLIGADO) {
    stringResource(R.string.privacidade_corte_desligado)
} else {
    Formatador.distancia(corte.metros.toLong())
}

@Composable
fun ZonasRoute(onAbrirZona: (String) -> Unit, viewModel: PrivacidadeRotasViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ZonasScreen(estado, onAbrirZona)
}

/** As zonas marcadas. Criar uma é a partir de uma rota gravada (sem GPS): a tela explica onde. */
@Composable
fun ZonasScreen(estado: PrivacidadeEstado, onAbrirZona: (String) -> Unit = {}) {
    TelaDiario(titulo = stringResource(R.string.zonas_titulo)) { padding ->
        val conteudo = Modifier.fillMaxSize().padding(padding)
        when (estado) {
            PrivacidadeEstado.Carregando -> EstadoCarregando(conteudo)
            is PrivacidadeEstado.Erro -> EstadoErro(estado.detalhe, conteudo)
            is PrivacidadeEstado.Pronto -> {
                val escala = LocalEscalaDiario.current
                Column(
                    conteudo.verticalScroll(rememberScrollState()).padding(escala.espaco(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(escala.espaco(8.dp)),
                ) {
                    Text(stringResource(R.string.zonas_explicacao), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.zonas_como_marcar),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (estado.protecao.zonas.isEmpty()) {
                        Text(stringResource(R.string.zonas_vazio), style = MaterialTheme.typography.titleMedium)
                    }
                    estado.protecao.zonas.forEach { ZonaItem(it, onAbrirZona) }
                }
            }
        }
    }
}

@Composable
private fun ZonaItem(zona: ZonaDePrivacidade, onAbrir: (String) -> Unit) {
    ListItem(
        headlineContent = { Text(zona.nome) },
        supportingContent = {
            Text(stringResource(R.string.zona_raio, Formatador.distancia(zona.raioM.toLong())))
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LocalEscalaDiario.current.alvoMinimo)
            .clickable(onClickLabel = stringResource(R.string.zona_editar)) { onAbrir(zona.id) },
    )
}
