package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.TelaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.core.ui.EstadoCarregando
import com.pedrohrsm.diariodebordo.core.ui.EstadoErro
import com.pedrohrsm.diariodebordo.core.ui.EstadoVazio
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.numeros.Formatador

@Composable
fun GaragemRoute(
    onAbrirMoto: (String) -> Unit,
    onNovaMoto: () -> Unit,
    viewModel: GaragemViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    GaragemScreen(estado = estado, onAbrirMoto = onAbrirMoto, onNovaMoto = onNovaMoto)
}

@Composable
fun GaragemScreen(estado: GaragemEstado, onAbrirMoto: (String) -> Unit = {}, onNovaMoto: () -> Unit = {}) {
    val escala = LocalEscalaDiario.current
    TelaDiario(
        titulo = stringResource(R.string.garagem_titulo),
        botaoFlutuante = {
            if (estado is GaragemEstado.Pronto && estado.podeAdicionar && estado.motos.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onNovaMoto,
                    modifier = Modifier.heightIn(min = escala.alvoMinimo),
                ) { Text(stringResource(R.string.garagem_adicionar)) }
            }
        },
    ) { padding ->
        val conteudo = Modifier.fillMaxSize().padding(padding)
        when (estado) {
            GaragemEstado.Carregando -> EstadoCarregando(conteudo)
            is GaragemEstado.Erro -> EstadoErro(estado.detalhe, conteudo)
            is GaragemEstado.Pronto -> if (estado.motos.isEmpty()) {
                EstadoVazio(
                    mensagem = stringResource(R.string.garagem_vazio),
                    modifier = conteudo,
                    rotuloAcao = stringResource(R.string.garagem_cadastrar),
                    onAcao = onNovaMoto,
                )
            } else {
                LazyColumn(contentPadding = padding) {
                    items(estado.motos, key = { it.id }) { moto ->
                        MotoItem(moto, estado.kmPorMoto[moto.id], onClick = { onAbrirMoto(moto.id) })
                    }
                    if (!estado.podeAdicionar) {
                        item {
                            ListItem(headlineContent = { Text(stringResource(R.string.garagem_limite_gratis)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MotoItem(moto: Moto, km: Int?, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick).heightIn(min = LocalEscalaDiario.current.alvoMinimo),
        headlineContent = { Text(moto.apelido) },
        supportingContent = { Text(stringResource(R.string.garagem_marca_modelo, moto.marca, moto.modelo)) },
        trailingContent = { if (km != null) Text(Formatador.km(km)) },
    )
}

@Preview
@Composable
private fun GaragemVaziaPreview() {
    DiarioTheme { GaragemScreen(estado = GaragemEstado.Pronto(emptyList())) }
}

@Preview
@Composable
private fun GaragemComMotoPreview() {
    val moto = Moto(id = "1", apelido = "Bandida", marca = "Harley-Davidson", modelo = "Sportster 883")
    DiarioTheme {
        GaragemScreen(estado = GaragemEstado.Pronto(listOf(moto), mapOf("1" to 23_450), podeAdicionar = false))
    }
}
