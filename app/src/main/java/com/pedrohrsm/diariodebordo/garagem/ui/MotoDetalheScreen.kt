package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.pedrohrsm.diariodebordo.garagem.dominio.PontoDoHodometro
import com.pedrohrsm.diariodebordo.numeros.Formatador

/**
 * Atalhos da moto (hub): cada funcionalidade liga o seu aqui. [atalhos] é desenhado no cabeçalho, abaixo
 * dos botões (o plano põe ali o atalho da troca de óleo do modo oficina).
 */
data class AcoesMoto(
    val onEditar: () -> Unit = {},
    val onDiario: () -> Unit = {},
    val atalhos: @Composable () -> Unit = {},
)

@Composable
fun MotoDetalheRoute(acoes: AcoesMoto, viewModel: MotoDetalheViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dialogo by viewModel.dialogo.collectAsStateWithLifecycle()
    val apagar by viewModel.apagar.collectAsStateWithLifecycle()
    MotoDetalheScreen(
        estado = estado,
        acoes = acoes,
        onAtualizarHodometro = viewModel::abrirHodometro,
        onEditarLeitura = viewModel::abrirCorrecao,
        onApagarLeitura = viewModel::pedirApagar,
    )
    apagar?.let {
        DialogoApagarLeituraView(
            confirmacao = it,
            onConfirmar = viewModel::confirmarApagar,
            onFechar = viewModel::cancelarApagar,
        )
    }
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
    onEditarLeitura: (LeituraHodometro) -> Unit = {},
    onApagarLeitura: (LeituraHodometro) -> Unit = {},
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
                items(estado.leituras, key = { it.id }) { LeituraItem(it, onEditarLeitura, onApagarLeitura) }
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
        acoes.atalhos()
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
private fun LeituraItem(
    leitura: LeituraHodometro,
    onEditar: (LeituraHodometro) -> Unit,
    onApagar: (LeituraHodometro) -> Unit,
) {
    val escala = LocalEscalaDiario.current
    ListItem(
        headlineContent = { Text(Formatador.km(leitura.km)) },
        supportingContent = {
            Column {
                Text(Formatador.data(leitura.lidaEm, Formatador.fuso(leitura.fusoHorario)))
                Row(horizontalArrangement = Arrangement.spacedBy(escala.espaco(8.dp))) {
                    TextButton(
                        onClick = { onEditar(leitura) },
                        modifier = Modifier.heightIn(min = escala.alvoMinimo),
                    ) { Text(stringResource(R.string.editar)) }
                    TextButton(
                        onClick = { onApagar(leitura) },
                        modifier = Modifier.heightIn(min = escala.alvoMinimo),
                    ) { Text(stringResource(R.string.apagar)) }
                }
            }
        },
    )
}

private fun textoDaVizinha(ponto: PontoDoHodometro): Pair<String, String> =
    Formatador.km(ponto.km) to Formatador.diaMes(ponto.lidaEm, Formatador.fuso(ponto.fusoHorario))

@Composable
private fun mensagemForaDoIntervalo(erro: ErroHodometro.ForaDoIntervalo): String {
    val anterior = erro.anterior
    val seguinte = erro.seguinte
    return when {
        anterior != null && seguinte != null -> {
            val (kmA, dataA) = textoDaVizinha(anterior)
            val (kmS, dataS) = textoDaVizinha(seguinte)
            stringResource(R.string.hodometro_entre, kmA, kmS, dataA, dataS)
        }
        anterior != null -> {
            val (kmA, dataA) = textoDaVizinha(anterior)
            stringResource(R.string.hodometro_acima_de, kmA, dataA)
        }
        seguinte != null -> {
            val (kmS, dataS) = textoDaVizinha(seguinte)
            stringResource(R.string.hodometro_abaixo_de, kmS, dataS)
        }
        else -> stringResource(R.string.km_invalido)
    }
}

@Composable
internal fun DialogoApagarLeituraView(confirmacao: ConfirmacaoApagar, onConfirmar: () -> Unit, onFechar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(stringResource(R.string.hodometro_apagar_titulo, Formatador.km(confirmacao.leitura.km))) },
        text = { TextoDialogoApagar(confirmacao) },
        confirmButton = {
            TextButton(onClick = onConfirmar, enabled = !confirmacao.apagando) {
                Text(stringResource(R.string.apagar))
            }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text(stringResource(R.string.cancelar)) } },
    )
}

/** Corpo do diálogo de apagar: o que sai do histórico e, se a regra recusou, o porquê. */
@Composable
internal fun TextoDialogoApagar(confirmacao: ConfirmacaoApagar) {
    val leitura = confirmacao.leitura
    val km = Formatador.km(leitura.km)
    val data = Formatador.data(leitura.lidaEm, Formatador.fuso(leitura.fusoHorario))
    val erro = when (val e = confirmacao.erro) {
        null -> null
        ErroApagar.UnicaComRegistrosDeKm -> stringResource(R.string.hodometro_apagar_unica)
        ErroApagar.LeituraSumiu -> stringResource(R.string.hodometro_leitura_sumiu)
        is ErroApagar.Falha -> stringResource(R.string.erro_ao_salvar, e.detalhe.orEmpty())
    }
    Column(verticalArrangement = Arrangement.spacedBy(LocalEscalaDiario.current.espaco(8.dp))) {
        Text(stringResource(R.string.hodometro_apagar_texto, "$km, $data"))
        erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
internal fun DialogoHodometroView(
    dialogo: DialogoHodometro,
    onTexto: (String) -> Unit,
    onSalvar: () -> Unit,
    onFechar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(stringResource(tituloDoDialogo(dialogo))) },
        text = { CampoDialogoHodometro(dialogo, onTexto) },
        confirmButton = {
            TextButton(onClick = onSalvar, enabled = !dialogo.salvando) { Text(stringResource(R.string.salvar)) }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text(stringResource(R.string.cancelar)) } },
    )
}

internal fun tituloDoDialogo(dialogo: DialogoHodometro): Int =
    if (dialogo.editando == null) R.string.hodometro_atualizar else R.string.hodometro_corrigir_titulo

/** Campo do km com a mensagem de erro da regra (inclusive "entre X e Y" da correção). */
@Composable
internal fun CampoDialogoHodometro(dialogo: DialogoHodometro, onTexto: (String) -> Unit) {
    val erro = when (val e = dialogo.erro) {
        null -> null
        ErroHodometro.Invalido -> stringResource(R.string.km_invalido)
        is ErroHodometro.Menor -> stringResource(R.string.hodometro_menor, Formatador.km(e.anteriorKm))
        is ErroHodometro.Igual -> stringResource(R.string.hodometro_igual, Formatador.km(e.anteriorKm))
        is ErroHodometro.Falha -> stringResource(R.string.erro_ao_salvar, e.detalhe.orEmpty())
        is ErroHodometro.ForaDoIntervalo -> mensagemForaDoIntervalo(e)
        ErroHodometro.LeituraSumiu -> stringResource(R.string.hodometro_leitura_sumiu)
    }
    CampoTexto(
        valor = dialogo.texto,
        onValor = onTexto,
        rotulo = stringResource(R.string.hodometro_km_atual),
        teclado = KeyboardType.Number,
        erro = erro,
    )
}

@Preview
@Composable
private fun MotoDetalhePreview() {
    val moto = Moto(id = "1", apelido = "Bandida", marca = "Harley-Davidson", modelo = "Sportster 883")
    val leitura = LeituraHodometro(motoId = "1", km = 23_450, lidaEm = 1_790_000_000_000)
    DiarioTheme { MotoDetalheScreen(estado = MotoDetalheEstado.Pronto(moto, listOf(leitura), kmAtual = leitura.km)) }
}
