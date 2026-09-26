package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val motos by viewModel.motos.collectAsStateWithLifecycle()
    GaragemScreen(motos = motos)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaragemScreen(motos: List<Moto>) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.garagem_titulo)) }) },
    ) { padding ->
        if (motos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.garagem_vazia))
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(), modifier = Modifier.padding(padding)) {
                items(motos, key = { it.id }) { moto ->
                    ListItem(
                        headlineContent = { Text(moto.apelido) },
                        supportingContent = { Text("${moto.marca} ${moto.modelo}") },
                        trailingContent = { Text(stringResource(R.string.garagem_km, moto.hodometroKm)) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun GaragemVaziaPreview() {
    DiarioTheme { GaragemScreen(motos = emptyList()) }
}
