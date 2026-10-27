package com.pedrohrsm.diariodebordo.diario.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.localizacao.PermissoesLocalizacao
import com.pedrohrsm.diariodebordo.numeros.Formatador

/**
 * "Usar local atual": a permissão é pedida só neste toque (nunca na abertura do app).
 * Negar não quebra nada: o registro salva sem local.
 */
@Composable
fun CampoLocal(estado: RegistroFormEstado, acoes: AcoesRegistroForm) {
    val contexto = LocalContext.current
    val escala = LocalEscalaDiario.current
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resposta ->
        if (resposta.values.any { it }) acoes.onUsarLocal() else acoes.onLocalNegado()
    }
    val lat = estado.latitude
    val lon = estado.longitude
    Text(stringResource(R.string.local_titulo), style = MaterialTheme.typography.labelLarge)
    if (lat != null && lon != null) {
        // Coordenada e botão em linhas próprias: lado a lado, a coordenada quebra com fonte em 200%.
        Text(Formatador.coordenada(lat, lon))
        TextButton(onClick = acoes.onRemoverLocal, modifier = Modifier.heightIn(min = escala.alvoMinimo)) {
            Text(stringResource(R.string.remover))
        }
    } else {
        OutlinedButton(
            onClick = {
                if (PermissoesLocalizacao.temAlguma(contexto)) {
                    acoes.onUsarLocal()
                } else {
                    pedir.launch(PermissoesLocalizacao.PEDIDO)
                }
            },
            enabled = !estado.buscandoLocal,
            modifier = Modifier.heightIn(min = escala.alvoMinimo),
        ) {
            Text(stringResource(if (estado.buscandoLocal) R.string.local_buscando else R.string.local_usar_atual))
        }
    }
}
