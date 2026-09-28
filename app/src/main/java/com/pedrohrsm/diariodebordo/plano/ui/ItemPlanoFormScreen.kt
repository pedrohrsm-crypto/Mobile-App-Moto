package com.pedrohrsm.diariodebordo.plano.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.LocalDimensoes
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.FormularioDiario
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.TelaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.ui.BotaoPrincipal
import com.pedrohrsm.diariodebordo.core.ui.CampoTexto
import com.pedrohrsm.diariodebordo.core.ui.EstadoCarregando
import com.pedrohrsm.diariodebordo.plano.dominio.CampoItem

@Composable
fun ItemPlanoFormRoute(onConcluido: () -> Unit, viewModel: ItemPlanoFormViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.concluido) { if (estado.concluido) onConcluido() }
    ItemPlanoFormScreen(estado, viewModel::alterar, viewModel::salvar, viewModel::excluir)
}

@Composable
fun ItemPlanoFormScreen(
    estado: ItemPlanoFormEstado,
    onAlterar: ((ItemPlanoFormEstado) -> ItemPlanoFormEstado) -> Unit = {},
    onSalvar: () -> Unit = {},
    onExcluir: () -> Unit = {},
) {
    TelaDiario(
        titulo = stringResource(if (estado.editando) R.string.item_plano_editar else R.string.item_plano_novo),
        acoes = {
            if (estado.editando) TextButton(onClick = onExcluir) { Text(stringResource(R.string.excluir)) }
        },
    ) { padding ->
        if (estado.carregando) {
            EstadoCarregando(Modifier.fillMaxSize().padding(padding))
        } else {
            FormularioDiario(padding) {
                Column(verticalArrangement = Arrangement.spacedBy(LocalDimensoes.current.espacoPequeno)) {
                    CampoTexto(
                        valor = estado.nome,
                        onValor = { v -> onAlterar { it.copy(nome = v) } },
                        rotulo = stringResource(R.string.item_plano_nome),
                        erro = stringResource(R.string.campo_obrigatorio).takeIf { CampoItem.NOME in estado.invalidos },
                    )
                    CampoTexto(
                        valor = estado.km,
                        onValor = { v -> onAlterar { it.copy(km = v) } },
                        rotulo = stringResource(R.string.item_plano_km),
                        teclado = KeyboardType.Number,
                        erro = stringResource(R.string.km_invalido).takeIf { CampoItem.KM in estado.invalidos },
                    )
                    CampoTexto(
                        valor = estado.dias,
                        onValor = { v -> onAlterar { it.copy(dias = v) } },
                        rotulo = stringResource(R.string.item_plano_dias),
                        teclado = KeyboardType.Number,
                        erro = stringResource(R.string.item_plano_dias_invalido)
                            .takeIf { CampoItem.DIAS in estado.invalidos },
                    )
                    if (CampoItem.INTERVALO in estado.invalidos) {
                        Text(stringResource(R.string.item_plano_sem_intervalo), color = MaterialTheme.colorScheme.error)
                    }
                    Text(stringResource(R.string.plano_dado_do_usuario), style = MaterialTheme.typography.bodySmall)
                    estado.erro?.let {
                        Text(stringResource(R.string.erro_ao_salvar, it), color = MaterialTheme.colorScheme.error)
                    }
                    BotaoPrincipal(stringResource(R.string.salvar), onSalvar, habilitado = !estado.salvando)
                }
            }
        }
    }
}

@Preview
@Composable
private fun ItemPlanoFormPreview() {
    DiarioTheme { ItemPlanoFormScreen(ItemPlanoFormEstado(carregando = false, nome = "Troca de óleo", km = "5000")) }
}
