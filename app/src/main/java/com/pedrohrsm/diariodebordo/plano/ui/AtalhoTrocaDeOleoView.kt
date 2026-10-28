package com.pedrohrsm.diariodebordo.plano.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.ui.BotaoPrincipal

/** Atalho da troca de óleo na tela da moto; só aparece no modo oficina (1.8). */
@Composable
fun AtalhoTrocaDeOleoRoute(viewModel: AtalhoTrocaDeOleoViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val acao by viewModel.acao.collectAsStateWithLifecycle()
    AtalhoTrocaDeOleoView(estado, acao, viewModel::pedir, viewModel::confirmar, viewModel::cancelar)
}

@Composable
fun AtalhoTrocaDeOleoView(
    estado: EstadoAtalhoOleo,
    acao: AcaoAtalhoOleo,
    onPedir: () -> Unit = {},
    onConfirmar: () -> Unit = {},
    onCancelar: () -> Unit = {},
) {
    when (estado) {
        EstadoAtalhoOleo.Oculto -> return
        is EstadoAtalhoOleo.Erro -> {
            Text(stringResource(R.string.erro_ao_carregar), color = MaterialTheme.colorScheme.error)
            return
        }
        EstadoAtalhoOleo.Disponivel -> Unit
    }
    Column {
        BotaoPrincipal(stringResource(R.string.oleo_atalho), onPedir, habilitado = !acao.gravando)
        when {
            acao.registrado -> Text(stringResource(R.string.oleo_atalho_registrado))
            acao.falha != null -> Text(
                stringResource(R.string.erro_ao_salvar, acao.falha),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
    if (acao.confirmando) {
        AlertDialog(
            onDismissRequest = onCancelar,
            text = { Text(stringResource(R.string.oleo_atalho_confirmar)) },
            confirmButton = { TextButton(onClick = onConfirmar) { Text(stringResource(R.string.oleo_atalho_sim)) } },
            dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) } },
        )
    }
}
