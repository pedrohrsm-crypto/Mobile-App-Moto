package com.pedrohrsm.diariodebordo.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario

/** Os três estados de toda tela (carregando, vazio, erro). Um erro nunca vira lista vazia. */
@Composable
fun EstadoCarregando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun EstadoVazio(
    mensagem: String,
    modifier: Modifier = Modifier,
    rotuloAcao: String? = null,
    onAcao: (() -> Unit)? = null,
) {
    val escala = LocalEscalaDiario.current
    Column(
        modifier.fillMaxSize().padding(escala.espaco(16.dp)),
        verticalArrangement = Arrangement.spacedBy(escala.espaco(16.dp), Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(mensagem, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        if (rotuloAcao != null && onAcao != null) {
            Button(onClick = onAcao, modifier = Modifier.heightIn(min = escala.alvoMinimo)) { Text(rotuloAcao) }
        }
    }
}

@Composable
fun EstadoErro(detalhe: String?, modifier: Modifier = Modifier) {
    val texto = stringResource(R.string.erro_ao_carregar) + (detalhe?.let { "\n$it" } ?: "")
    EstadoVazio(mensagem = texto, modifier = modifier)
}

/** Quem ainda não cadastrou moto: toda aba leva ao cadastro. */
@Composable
fun EstadoSemMoto(onCadastrarMoto: () -> Unit, modifier: Modifier = Modifier) {
    EstadoVazio(
        mensagem = stringResource(R.string.sem_moto_mensagem),
        modifier = modifier,
        rotuloAcao = stringResource(R.string.garagem_cadastrar),
        onAcao = onCadastrarMoto,
    )
}
