package com.pedrohrsm.diariodebordo.exemplo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.garagem.ui.BotaoSecundario

/** Só no build debug (#77): botão que grava motos, registros, plano e rotas inventados, para capturas e vídeo. */
@Composable
fun SecaoDadosDeExemplo(viewModel: ExemploViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(LocalEscalaDiario.current.espaco(8.dp))) {
        Text(stringResource(R.string.exemplo_titulo), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.exemplo_explicacao), style = MaterialTheme.typography.bodyMedium)
        BotaoSecundario(stringResource(R.string.exemplo_carregar), viewModel::carregar)
        when (val e = estado) {
            EstadoExemplo.Ocioso -> Unit
            EstadoExemplo.Carregando -> Text(stringResource(R.string.exemplo_carregando))
            EstadoExemplo.Falhou -> Text(
                stringResource(R.string.exemplo_falhou),
                color = MaterialTheme.colorScheme.error,
            )
            is EstadoExemplo.Carregado -> Text(
                pluralStringResource(R.plurals.exemplo_carregado, e.motos, e.motos, e.registros),
            )
        }
    }
}
