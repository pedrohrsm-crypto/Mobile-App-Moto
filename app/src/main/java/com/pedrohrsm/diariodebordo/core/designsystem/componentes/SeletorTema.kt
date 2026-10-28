package com.pedrohrsm.diariodebordo.core.designsystem.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario

/**
 * Escolha do tema e do modo oficina, como componente reutilizável (a tela de ajustes da Fase 1 o usa).
 * A troca vale na hora, na mesma Activity: o estado vive acima do [DiarioTheme].
 */
@Composable
fun SeletorTema(ajuste: AjusteTema, aoAjustar: (AjusteTema) -> Unit, modifier: Modifier = Modifier) {
    val escala = LocalEscalaDiario.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(escala.espaco(8.dp))) {
        Text(stringResource(R.string.ajuste_tema_grupo), style = MaterialTheme.typography.titleMedium)
        Column(Modifier.selectableGroup()) {
            TemaDiario.entries.forEach { tema ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = escala.alvoMinimo)
                        .selectable(
                            selected = tema == ajuste.tema,
                            onClick = { aoAjustar(ajuste.copy(tema = tema)) },
                            role = Role.RadioButton,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = tema == ajuste.tema, onClick = null)
                    Text(
                        text = stringResource(tema.rotulo()),
                        modifier = Modifier.padding(start = escala.espaco(16.dp)),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = escala.alvoMinimo)
                .toggleable(
                    value = ajuste.modoOficina,
                    onValueChange = { aoAjustar(ajuste.copy(modoOficina = it)) },
                    role = Role.Switch,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.modo_oficina), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.modo_oficina_descricao),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = ajuste.modoOficina, onCheckedChange = null, colors = coresSwitchDiario())
        }
    }
}

/**
 * Cores do Switch. O polegar desligado do Material é `outline` sobre `surfaceContainerHighest`: de 2,85 a 2,91:1 (mínimo 3
 * para componente de interface). `onSurfaceVariant` passa nos 3 temas. O `ContrasteComponentesTest` lê esta função.
 */
@Composable
internal fun coresSwitchDiario(): SwitchColors =
    SwitchDefaults.colors(uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant)

private fun TemaDiario.rotulo(): Int = when (this) {
    TemaDiario.OFICINA -> R.string.tema_oficina
    TemaDiario.ESTRADA -> R.string.tema_estrada
    TemaDiario.CROMO -> R.string.tema_cromo
}
