package com.pedrohrsm.diariodebordo.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.backup.ui.SecaoBackup
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.SeletorTema
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.TelaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.core.premium.RecursoPremium

@Composable
fun AjustesRoute(viewModel: AjustesViewModel = hiltViewModel()) {
    val ajuste by viewModel.ajuste.collectAsStateWithLifecycle()
    AjustesScreen(ajuste, viewModel::ajustar, viewModel.recursosPremium) { SecaoBackup() }
}

/** Aparência (1.8), backup (1.9) e, só com a flag ligada, o que o premium libera (1.10). */
@Composable
fun AjustesScreen(
    ajuste: AjusteTema,
    onAjustar: (AjusteTema) -> Unit = {},
    recursosPremium: Set<RecursoPremium> = emptySet(),
    backup: @Composable () -> Unit = {},
) {
    val escala = LocalEscalaDiario.current
    TelaDiario(titulo = stringResource(R.string.ajustes_titulo)) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(escala.espaco(16.dp)),
            verticalArrangement = Arrangement.spacedBy(escala.espaco(16.dp)),
        ) {
            SeletorTema(ajuste = ajuste, aoAjustar = onAjustar)
            HorizontalDivider()
            backup()
            if (recursosPremium.isNotEmpty()) {
                HorizontalDivider()
                SecaoPremium(recursosPremium)
            }
        }
    }
}

@Composable
private fun SecaoPremium(recursos: Set<RecursoPremium>) {
    Column(verticalArrangement = Arrangement.spacedBy(LocalEscalaDiario.current.espaco(8.dp))) {
        Text(stringResource(R.string.premium_titulo), style = MaterialTheme.typography.titleMedium)
        recursos.forEach { Text(stringResource(it.rotulo()), style = MaterialTheme.typography.bodyLarge) }
    }
}

private fun RecursoPremium.rotulo(): Int = when (this) {
    RecursoPremium.MOTOS_ILIMITADAS -> R.string.premium_motos_ilimitadas
    RecursoPremium.BACKUP_NUVEM -> R.string.premium_backup_nuvem
    RecursoPremium.RELATORIO_PDF -> R.string.premium_relatorio_pdf
    RecursoPremium.ESTATISTICAS -> R.string.premium_estatisticas
}
