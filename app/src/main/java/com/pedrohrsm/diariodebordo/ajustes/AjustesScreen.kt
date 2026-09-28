package com.pedrohrsm.diariodebordo.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.LocalDimensoes
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.TelaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.core.ui.EscolhaUnica

@Composable
fun AjustesRoute(viewModel: AjustesViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    AjustesScreen(estado, viewModel::escolherTema, viewModel::alternarModoOficina)
}

@Composable
fun AjustesScreen(estado: AjustesEstado, onTema: (TemaDiario) -> Unit = {}, onModoOficina: (Boolean) -> Unit = {}) {
    val d = LocalDimensoes.current
    TelaDiario(titulo = stringResource(R.string.ajustes_titulo)) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(d.espaco),
            verticalArrangement = Arrangement.spacedBy(d.espacoPequeno),
        ) {
            Text(stringResource(R.string.ajustes_tema), style = MaterialTheme.typography.titleMedium)
            EscolhaUnica(
                opcoes = TemaDiario.entries,
                selecionada = estado.tema,
                rotulo = { rotuloTema(it) },
                onEscolher = { it?.let(onTema) },
                permiteNenhuma = false,
            )
            // A linha inteira liga e desliga: alvo grande para quem está de luva.
            ListItem(
                modifier = Modifier
                    .heightIn(min = d.toqueMinimo)
                    .toggleable(value = estado.modoOficina, role = Role.Switch, onValueChange = onModoOficina),
                headlineContent = { Text(stringResource(R.string.ajustes_modo_oficina)) },
                supportingContent = { Text(stringResource(R.string.ajustes_modo_oficina_explicacao)) },
                trailingContent = { Switch(checked = estado.modoOficina, onCheckedChange = null) },
            )
        }
    }
}

@Composable
private fun rotuloTema(tema: TemaDiario): String = stringResource(
    when (tema) {
        TemaDiario.OFICINA -> R.string.tema_oficina
        TemaDiario.ESTRADA -> R.string.tema_estrada
        TemaDiario.CROMO -> R.string.tema_cromo
    },
)

@Preview
@Composable
private fun AjustesPreview() {
    DiarioTheme(modoOficina = true) { AjustesScreen(AjustesEstado(modoOficina = true)) }
}
