package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.garagem.data.Moto

@Composable
fun GaragemRoute(viewModel: GaragemViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    GaragemScreen(estado = estado)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaragemScreen(estado: GaragemEstado) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.garagem_titulo)) }) },
    ) { padding ->
        val centro = Modifier.fillMaxSize().padding(padding)
        when (estado) {
            GaragemEstado.Carregando -> Box(centro, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is GaragemEstado.Pronto -> if (estado.motos.isEmpty()) {
                Box(centro, contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.garagem_vazio))
                }
            } else {
                LazyColumn(modifier = Modifier.padding(padding)) {
                    items(estado.motos, key = { it.id }) { moto -> MotoItem(moto) }
                }
            }
        }
    }
}

@Composable
private fun MotoItem(moto: Moto) {
    ListItem(
        headlineContent = { Text(moto.apelido) },
        supportingContent = { Text(stringResource(R.string.garagem_marca_modelo, moto.marca, moto.modelo)) },
    )
}

@Preview
@Composable
private fun GaragemVaziaPreview() {
    DiarioTheme { GaragemScreen(estado = GaragemEstado.Pronto(emptyList())) }
}
