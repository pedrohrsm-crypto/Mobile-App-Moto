package com.pedrohrsm.diariodebordo.rota.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.TelaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.core.ui.EstadoCarregando
import com.pedrohrsm.diariodebordo.core.ui.EstadoErro
import com.pedrohrsm.diariodebordo.core.ui.EstadoVazio
import com.pedrohrsm.diariodebordo.garagem.ui.BotaoSecundario
import com.pedrohrsm.diariodebordo.numeros.Formatador
import com.pedrohrsm.diariodebordo.rota.dominio.PontaDaRota

@Composable
fun RotaDetalheRoute(
    onExcluida: () -> Unit,
    onMarcarZona: (PontaDaRota) -> Unit,
    viewModel: RotaDetalheViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val excluida by viewModel.excluida.collectAsStateWithLifecycle()
    val falha by viewModel.falha.collectAsStateWithLifecycle()
    LaunchedEffect(excluida) { if (excluida) onExcluida() }
    RotaDetalheTela(estado, onExcluir = viewModel::excluir, onMarcarZona = onMarcarZona)
    falha?.let {
        AlertDialog(
            onDismissRequest = viewModel::limparFalha,
            text = { Text(stringResource(R.string.erro_ao_salvar, it)) },
            confirmButton = { TextButton(onClick = viewModel::limparFalha) { Text(stringResource(R.string.ok)) } },
        )
    }
}

/**
 * Data, distância, duração e o traçado sem mapa, com o que sai oculto no arquivo exportado em cinza (#78). Daqui
 * a pessoa marca uma zona de privacidade no começo ou no fim da rota, sem GPS. Excluir pede confirmação.
 */
@Composable
fun RotaDetalheTela(estado: RotaDetalheEstado, onExcluir: () -> Unit = {}, onMarcarZona: (PontaDaRota) -> Unit = {}) {
    var confirmar by remember { mutableStateOf(false) }
    TelaDiario(
        titulo = stringResource(R.string.rota_titulo),
        acoes = {
            if (estado is RotaDetalheEstado.Pronto) {
                TextButton(
                    onClick = { confirmar = true },
                    modifier = Modifier.heightIn(min = LocalEscalaDiario.current.alvoMinimo),
                ) { Text(stringResource(R.string.excluir)) }
            }
        },
    ) { padding ->
        val conteudo = Modifier.fillMaxSize().padding(padding)
        when (estado) {
            RotaDetalheEstado.Carregando -> EstadoCarregando(conteudo)
            RotaDetalheEstado.NaoEncontrada -> EstadoVazio(stringResource(R.string.rota_nao_encontrada), conteudo)
            is RotaDetalheEstado.Erro -> EstadoErro(estado.detalhe, conteudo)
            is RotaDetalheEstado.Pronto -> {
                val escala = LocalEscalaDiario.current
                Column(
                    conteudo.verticalScroll(rememberScrollState()).padding(escala.espaco(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(escala.espaco(16.dp)),
                ) {
                    Text(
                        Formatador.dataHora(estado.rota.iniciadaEm, Formatador.fuso(estado.rota.fusoHorario)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        Formatador.distancia(estado.rota.distanciaM) + " · " +
                            Formatador.duracao(estado.rota.duracaoS),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    TracadoRota(estado.trechos, stringResource(R.string.rota_tracado_descricao))
                    if (estado.trechos.any { it.oculto }) {
                        Text(stringResource(R.string.rota_trecho_oculto), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(stringResource(R.string.rota_sem_mapa), style = MaterialTheme.typography.bodySmall)
                    if (estado.trechos.isNotEmpty()) {
                        PontaDaRota.entries.forEach { ponta ->
                            BotaoSecundario(stringResource(ponta.rotuloMarcar()), { onMarcarZona(ponta) })
                        }
                    }
                }
            }
        }
    }
    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text(stringResource(R.string.rota_excluir_titulo)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmar = false
                    onExcluir()
                }) { Text(stringResource(R.string.excluir)) }
            },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
}

private fun PontaDaRota.rotuloMarcar(): Int = when (this) {
    PontaDaRota.INICIO -> R.string.rota_marcar_zona_inicio
    PontaDaRota.FIM -> R.string.rota_marcar_zona_fim
}
