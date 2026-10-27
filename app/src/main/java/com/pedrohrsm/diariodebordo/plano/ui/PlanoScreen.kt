package com.pedrohrsm.diariodebordo.plano.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.TelaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.core.ui.EstadoCarregando
import com.pedrohrsm.diariodebordo.core.ui.EstadoErro
import com.pedrohrsm.diariodebordo.core.ui.EstadoSemMoto
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.numeros.Formatador
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.OrigemItem
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import java.time.Clock
import kotlin.math.abs

data class AcoesPlano(
    val onNovoItem: () -> Unit = {},
    val onEditarItem: (String) -> Unit = {},
    val onMarcarFeito: (ItemDoPlano) -> Unit = {},
    val onResolver: (String) -> Unit = {},
    val onCadastrarMoto: () -> Unit = {},
)

@Composable
fun PlanoRoute(acoes: AcoesPlano, destaque: String? = null, viewModel: PlanoViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val falha by viewModel.falha.collectAsStateWithLifecycle()
    PlanoScreen(estado, acoes.copy(onResolver = viewModel::resolver, onMarcarFeito = viewModel::marcarFeito), destaque)
    falha?.let { detalhe ->
        AlertDialog(
            onDismissRequest = viewModel::limparFalha,
            text = { Text(stringResource(R.string.erro_ao_salvar, detalhe)) },
            confirmButton = { TextButton(onClick = viewModel::limparFalha) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@Composable
fun PlanoScreen(estado: PlanoEstado, acoes: AcoesPlano = AcoesPlano(), destaque: String? = null) {
    val escala = LocalEscalaDiario.current
    val lista = rememberLazyListState()
    // Aberto por um lembrete: rola até o item avisado.
    LaunchedEffect(estado is PlanoEstado.Pronto, destaque) {
        val pronto = estado as? PlanoEstado.Pronto ?: return@LaunchedEffect
        val indice = pronto.itens.indexOfFirst { it.item.id == destaque }
        // Antes dos itens: o aviso de permissão, o cabeçalho de pendências com as pendências (se houver) e o cabeçalho.
        val antes = 2 + if (pronto.pendencias.isEmpty()) 0 else pronto.pendencias.size + 1
        if (indice >= 0) lista.animateScrollToItem(antes + indice)
    }
    val titulo = (estado as? PlanoEstado.Pronto)?.moto?.apelido
        ?.let { stringResource(R.string.plano_titulo_moto, it) }
        ?: stringResource(R.string.plano_titulo)
    TelaDiario(
        titulo = titulo,
        botaoFlutuante = {
            if (estado is PlanoEstado.Pronto) {
                ExtendedFloatingActionButton(
                    onClick = acoes.onNovoItem,
                    modifier = Modifier.heightIn(min = escala.alvoMinimo),
                ) {
                    Text(stringResource(R.string.item_plano_novo))
                }
            }
        },
    ) { padding ->
        val conteudo = Modifier.fillMaxSize().padding(padding)
        when (estado) {
            PlanoEstado.Carregando -> EstadoCarregando(conteudo)
            PlanoEstado.SemMoto -> EstadoSemMoto(acoes.onCadastrarMoto, conteudo)
            is PlanoEstado.Erro -> EstadoErro(estado.detalhe, conteudo)
            is PlanoEstado.Pronto -> LazyColumn(state = lista, contentPadding = padding) {
                item { AvisoPermissaoNotificacao() }
                if (estado.pendencias.isNotEmpty()) {
                    item { Secao(stringResource(R.string.plano_pendencias)) }
                    items(estado.pendencias, key = { it.id }) { PendenciaItem(it, acoes.onResolver) }
                }
                item { Secao(stringResource(R.string.plano_itens)) }
                items(estado.itens, key = { it.item.id }) { ItemPeriodico(it, acoes, it.item.id == destaque) }
                item {
                    Text(
                        stringResource(R.string.plano_dado_do_usuario),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(escala.espaco(16.dp)),
                    )
                }
                // Folga para o botão flutuante não cobrir o último item.
                item { Spacer(Modifier.height(escala.alvoMinimo + escala.espaco(32.dp))) }
            }
        }
    }
}

@Composable
private fun Secao(texto: String) {
    ListItem(headlineContent = { Text(texto, style = MaterialTheme.typography.titleMedium) })
}

@Composable
private fun PendenciaItem(item: ItemDoPlano, onResolver: (String) -> Unit) {
    ListItem(
        headlineContent = { Text(item.nome) },
        // O botão fica abaixo do texto, e não ao lado: com fonte grande o texto ainda usa a largura toda.
        supportingContent = {
            Column {
                Text(stringResource(R.string.plano_pendencia_origem))
                TextButton(
                    onClick = { onResolver(item.id) },
                    modifier = Modifier.heightIn(min = LocalEscalaDiario.current.alvoMinimo),
                ) { Text(stringResource(R.string.plano_resolver)) }
            }
        },
    )
}

@Composable
private fun ItemPeriodico(itemComSituacao: ItemComSituacao, acoes: AcoesPlano, destacado: Boolean) {
    val (item, situacao) = itemComSituacao
    val proxima = listOfNotNull(
        situacao.proxima.km?.let { Formatador.km(it) },
        situacao.proxima.data?.let { Formatador.data(it) },
    ).joinToString(stringResource(R.string.plano_ou))
    val restante = listOfNotNull(
        situacao.situacao.kmRestantes?.let { restanteTexto(Formatador.km(abs(it)), faltam = it >= 0) },
        situacao.situacao.diasRestantes?.let {
            restanteTexto(pluralStringResource(R.plurals.plano_dias, abs(it), abs(it)), faltam = it >= 0)
        },
    ).joinToString(stringResource(R.string.plano_separador))
    val (textoUrgencia, cor) = when (situacao.urgencia) {
        Urgencia.VENCIDO -> stringResource(R.string.plano_vencido) to MaterialTheme.colorScheme.error
        Urgencia.PROXIMO -> stringResource(R.string.plano_proximo) to MaterialTheme.colorScheme.primary
        Urgencia.EM_DIA -> stringResource(R.string.plano_em_dia) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val alvo = LocalEscalaDiario.current.alvoMinimo
    ListItem(
        modifier = Modifier.clickable { acoes.onEditarItem(item.id) }.heightIn(min = alvo),
        colors = if (destacado) {
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            ListItemDefaults.colors()
        },
        overlineContent = { Text(textoUrgencia, color = cor) },
        headlineContent = { Text(item.nome) },
        supportingContent = {
            val desde = if (item.feitoEm == null) stringResource(R.string.plano_desde_criacao) else ""
            Column {
                Text(stringResource(R.string.plano_proxima, proxima) + "\n" + restante + desde)
                TextButton(
                    onClick = { acoes.onMarcarFeito(item) },
                    modifier = Modifier.heightIn(min = alvo),
                ) { Text(stringResource(R.string.plano_marcar_feito)) }
            }
        },
    )
}

/** "faltam 500 km" ou "passou 2 dias": quanto falta, ou quanto passou, de um dos intervalos do item. */
@Composable
private fun restanteTexto(valor: String, faltam: Boolean): String =
    stringResource(if (faltam) R.string.plano_faltam else R.string.plano_passou, valor)

@Preview
@Composable
private fun PlanoPreview() {
    val moto = Moto(id = "1", apelido = "Bandida", marca = "Harley-Davidson", modelo = "883")
    val itens = listOf(
        ItemDoPlano(motoId = "1", nome = "Troca de óleo", intervaloKm = 5_000, ultimaVezKm = 10_000, ultimaVezEm = 0),
        ItemDoPlano(motoId = "1", nome = "Conferir manetes", origem = OrigemItem.OCORRENCIA),
    )
    DiarioTheme { PlanoScreen(montarPlano(moto, itens, 14_800, Clock.systemDefaultZone())) }
}
