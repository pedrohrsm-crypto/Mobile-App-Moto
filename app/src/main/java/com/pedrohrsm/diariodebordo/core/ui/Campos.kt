package com.pedrohrsm.diariodebordo.core.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
