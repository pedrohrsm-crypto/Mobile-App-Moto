package com.pedrohrsm.diariodebordo.core.designsystem.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Casca de toda tela do app. O app é edge-to-edge (MainActivity chama enableEdgeToEdge):
 * o Scaffold desconta as barras do sistema, e quem desenha o conteúdo usa o [PaddingValues] recebido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDiario(
    titulo: String,
    modifier: Modifier = Modifier,
    acoes: @Composable RowScope.() -> Unit = {},
    botaoFlutuante: @Composable () -> Unit = {},
    conteudo: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(titulo) }, actions = acoes) },
        floatingActionButton = botaoFlutuante,
        content = conteudo,
    )
}

/**
 * Corpo de formulário: rola e sobe junto com o teclado, para nenhum campo ficar coberto em 360 dp.
 * Use dentro de [TelaDiario], passando o padding recebido dela.
 */
@Composable
fun FormularioDiario(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
            .consumeWindowInsets(padding)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        content = conteudo,
    )
}
