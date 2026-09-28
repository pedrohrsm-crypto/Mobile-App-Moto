package com.pedrohrsm.diariodebordo.rota.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.LocalDimensoes
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.TelaDiario
import com.pedrohrsm.diariodebordo.core.ui.EstadoCarregando
import com.pedrohrsm.diariodebordo.core.ui.EstadoErro
import com.pedrohrsm.diariodebordo.core.ui.EstadoVazio
import com.pedrohrsm.diariodebordo.numeros.Formatador

@Composable
fun RotaDetalheRoute(onExcluida: () -> Unit, viewModel: RotaDetalheViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val excluida by viewModel.excluida.collectAsStateWithLifecycle()
    val falha by viewModel.falha.collectAsStateWithLifecycle()
    LaunchedEffect(excluida) { if (excluida) onExcluida() }
    var confirmar by remember { mutableStateOf(false) }
    TelaDiario(
        titulo = stringResource(R.string.rota_titulo),
        acoes = {
            if (estado is RotaDetalheEstado.Pronto) {
                TextButton(onClick = { confirmar = true }) { Text(stringResource(R.string.excluir)) }
            }
        },
    ) { padding ->
        val conteudo = Modifier.fillMaxSize().padding(padding)
        when (val e = estado) {
            RotaDetalheEstado.Carregando -> EstadoCarregando(conteudo)
            RotaDetalheEstado.NaoEncontrada -> EstadoVazio(stringResource(R.string.rota_nao_encontrada), conteudo)
            is RotaDetalheEstado.Erro -> EstadoErro(e.detalhe, conteudo)
            is RotaDetalheEstado.Pronto -> {
                val d = LocalDimensoes.current
                Column(
                    conteudo.verticalScroll(rememberScrollState()).padding(d.espaco),
                    verticalArrangement = Arrangement.spacedBy(d.espaco),
                ) {
                    Text(
                        Formatador.dataHora(e.rota.iniciadaEm, Formatador.fuso(e.rota.fusoHorario)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        Formatador.distancia(e.rota.distanciaM) + " · " + Formatador.duracao(e.rota.duracaoS),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    TracadoRota(e.rota.polilinha, stringResource(R.string.rota_tracado_descricao))
                    Text(stringResource(R.string.rota_sem_mapa), style = MaterialTheme.typography.bodySmall)
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
                    viewModel.excluir()
                }) { Text(stringResource(R.string.excluir)) }
            },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
    falha?.let {
        AlertDialog(
            onDismissRequest = {},
            text = { Text(stringResource(R.string.erro_ao_salvar, it)) },
            confirmButton = { TextButton(onClick = onExcluida) { Text(stringResource(R.string.ok)) } },
        )
    }
}
