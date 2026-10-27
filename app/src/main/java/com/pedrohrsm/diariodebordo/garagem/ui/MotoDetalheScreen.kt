package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.TelaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.core.ui.BotaoPrincipal
import com.pedrohrsm.diariodebordo.core.ui.CampoTexto
import com.pedrohrsm.diariodebordo.core.ui.EstadoCarregando
import com.pedrohrsm.diariodebordo.core.ui.EstadoErro
import com.pedrohrsm.diariodebordo.core.ui.EstadoVazio
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.numeros.Formatador

/** Atalhos da moto (hub): cada funcionalidade liga o seu aqui. */
data class AcoesMoto(val onEditar: () -> Unit = {}, val onDiario: () -> Unit = {})

@Composable
fun MotoDetalheRoute(acoes: AcoesMoto, viewModel: MotoDetalheViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dialogo by viewModel.dialogo.collectAsStateWithLifecycle()
    MotoDetalheScreen(estado = estado, acoes = acoes, onAtualizarHodometro = viewModel::abrirHodometro)
    dialogo?.let {
        DialogoHodometroView(
            dialogo = it,
            onTexto = viewModel::alterarHodometro,
            onSalvar = viewModel::salvarHodometro,
            onFechar = viewModel::fecharHodometro,
        )
    }
}

@Composable
fun MotoDetalheScreen(
    estado: MotoDetalheEstado,
    acoes: AcoesMoto = AcoesMoto(),
    onAtualizarHodometro: () -> Unit = {},
) {
    val titulo = (estado as? MotoDetalheEstado.Pronto)?.moto?.apelido ?: stringResource(R.string.moto_titulo)
    TelaDiario(
        titulo = titulo,
        acoes = {
            if (estado is MotoDetalheEstado.Pronto) {
                TextButton(onClick = acoes.onEditar) { Text(stringResource(R.string.editar)) }
            }
        },
    ) { padding ->
        val conteudo = Modifier.fillMaxSize().padding(padding)
        when (estado) {
            MotoDetalheEstado.Carregando -> EstadoCarregando(conteudo)
            MotoDetalheEstado.NaoEncontrada -> EstadoVazio(stringResource(R.string.moto_nao_encontrada), conteudo)
            is MotoDetalheEstado.Erro -> EstadoErro(estado.detalhe, conteudo)
            is MotoDetalheEstado.Pronto -> LazyColumn(contentPadding = padding) {
                item { Cabecalho(estado, acoes, onAtualizarHodometro) }
                item {
                    ListItem(
                        headlineContent = {
                            Text(
                                stringResource(R.string.hodometro_historico),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        },
                    )
                }
                items(estado.leituras, key = { it.id }) { LeituraItem(it) }
            }
        }
    }
}

@Composable
private fun Cabecalho(estado: MotoDetalheEstado.Pronto, acoes: AcoesMoto, onAtualizarHodometro: () -> Unit) {
    val escala = LocalEscalaDiario.current
    val moto = estado.moto
    Column(
        Modifier.fillMaxWidth().padding(escala.espaco(16.dp)),
        verticalArrangement = Arrangement.spacedBy(escala.espaco(8.dp)),
    ) {
        Text(stringResource(R.string.garagem_marca_modelo, moto.marca, moto.modelo))
        Text(
            text = estado.kmAtual?.let { Formatador.km(it) } ?: stringResource(R.string.hodometro_sem_leitura),
            style = MaterialTheme.typography.headlineMedium,
        )
        BotaoPrincipal(stringResource(R.string.hodometro_atualizar), onAtualizarHodometro)
        BotaoSecundario(stringResource(R.string.diario_titulo), acoes.onDiario)
    }
}

@Composable
fun BotaoSecundario(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = LocalEscalaDiario.current.alvoMinimo),
    ) { Text(texto) }
}

@Composable
private fun LeituraItem(leitura: LeituraHodometro) {
    ListItem(
        headlineContent = { Text(Formatador.km(leitura.km)) },
        supportingContent = { Text(Formatador.data(leitura.lidaEm, Formatador.fuso(leitura.fusoHorario))) },
    )
}

@Composable
private fun DialogoHodometroView(
    dialogo: DialogoHodometro,
    onTexto: (String) -> Unit,
    onSalvar: () -> Unit,
    onFechar: () -> Unit,
) {
    val erro = when (val e = dialogo.erro) {
        null -> null
        ErroHodometro.Invalido -> stringResource(R.string.km_invalido)
        is ErroHodometro.Menor -> stringResource(R.string.hodometro_menor, Formatador.km(e.anteriorKm))
        is ErroHodometro.Igual -> stringResource(R.string.hodometro_igual, Formatador.km(e.anteriorKm))
        is ErroHodometro.Falha -> stringResource(R.string.erro_ao_salvar, e.detalhe.orEmpty())
    }
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(stringResource(R.string.hodometro_atualizar)) },
        text = {
            CampoTexto(
                valor = dialogo.texto,
                onValor = onTexto,
                rotulo = stringResource(R.string.hodometro_km_atual),
                teclado = KeyboardType.Number,
                erro = erro,
            )
        },
        confirmButton = {
            TextButton(onClick = onSalvar, enabled = !dialogo.salvando) { Text(stringResource(R.string.salvar)) }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text(stringResource(R.string.cancelar)) } },
    )
}

@Preview
@Composable
private fun MotoDetalhePreview() {
    val moto = Moto(id = "1", apelido = "Bandida", marca = "Harley-Davidson", modelo = "Sportster 883")
    val leitura = LeituraHodometro(motoId = "1", km = 23_450, lidaEm = 1_790_000_000_000)
    DiarioTheme { MotoDetalheScreen(estado = MotoDetalheEstado.Pronto(moto, listOf(leitura))) }
}
