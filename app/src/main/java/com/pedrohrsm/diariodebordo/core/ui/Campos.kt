package com.pedrohrsm.diariodebordo.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario

/** Campo de texto padrão dos formulários: rótulo, erro abaixo e altura mínima de toque. */
@Composable
fun CampoTexto(
    valor: String,
    onValor: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    erro: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    linhas: Int = 1,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValor,
        label = { Text(rotulo) },
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        singleLine = linhas == 1,
        minLines = linhas,
        keyboardOptions = KeyboardOptions(
            keyboardType = teclado,
            capitalization = if (teclado == KeyboardType.Text) {
                KeyboardCapitalization.Sentences
            } else {
                KeyboardCapitalization.None
            },
        ),
        modifier = modifier.fillMaxWidth().heightIn(min = LocalEscalaDiario.current.alvoMinimo),
    )
}

/**
 * Borda do chip. O padrão do Material (`outlineVariant` no não selecionado, nenhuma no selecionado) dava de 1,35 a
 * 1,61:1 sobre o fundo, e o chip selecionado só se distinguia pelo preenchimento (1,10 a 1,40:1; no modo oficina o
 * rótulo também é igual). Não selecionado: `outline` de 1 dp (3,17 a 3,83:1). Selecionado: `primary` de 2 dp. O
 * `ContrasteComponentesTest` lê esta função.
 */
@Composable
internal fun bordaChipDiario(selecionado: Boolean): BorderStroke {
    val cores = MaterialTheme.colorScheme
    return FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = selecionado,
        borderColor = cores.outline,
        selectedBorderColor = cores.primary,
        borderWidth = 1.dp,
        selectedBorderWidth = 2.dp,
    )
}

/** Escolha única entre poucas opções, em chips que rolam na horizontal em telas estreitas. */
@Composable
fun <T> EscolhaUnica(
    opcoes: List<T>,
    selecionada: T?,
    rotulo: @Composable (T) -> String,
    onEscolher: (T?) -> Unit,
    modifier: Modifier = Modifier,
    permiteNenhuma: Boolean = true,
) {
    val escala = LocalEscalaDiario.current
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(escala.espaco(8.dp)),
    ) {
        opcoes.forEach { opcao ->
            val marcada = opcao == selecionada
            FilterChip(
                selected = marcada,
                onClick = { onEscolher(if (marcada && permiteNenhuma) null else opcao) },
                label = { Text(rotulo(opcao)) },
                modifier = Modifier.heightIn(min = escala.alvoMinimo),
                border = bordaChipDiario(marcada),
            )
        }
    }
}

@Composable
fun BotaoPrincipal(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.fillMaxWidth().heightIn(min = LocalEscalaDiario.current.alvoMinimo),
    ) { Text(texto, style = MaterialTheme.typography.titleMedium) }
}
