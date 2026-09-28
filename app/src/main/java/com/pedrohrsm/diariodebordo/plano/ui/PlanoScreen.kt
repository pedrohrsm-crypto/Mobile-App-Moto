package com.pedrohrsm.diariodebordo.plano.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.LocalDimensoes
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.TelaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.ui.EstadoCarregando
import com.pedrohrsm.diariodebordo.core.ui.EstadoErro
import com.pedrohrsm.diariodebordo.core.ui.EstadoSemMoto
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.numeros.Formatador
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.OrigemItem
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import java.time.Clock
import java.time.format.DateTimeFormatter

data class AcoesPlano(
    val onNovoItem: () -> Unit = {},
    val onEditarItem: (String) -> Unit = {},
    val onRegistrarFeito: (ItemDoPlano) -> Unit = {},
    val onResolver: (String) -> Unit = {},
    val onCadastrarMoto: () -> Unit = {},
)

@Composable
fun PlanoRoute(acoes: AcoesPlano, viewModel: PlanoViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val falha by viewModel.falha.collectAsStateWithLifecycle()
    PlanoScreen(estado, acoes.copy(onResolver = viewModel::resolver))
    falha?.let { detalhe ->
        AlertDialog(
            onDismissRequest = viewModel::limparFalha,
            text = { Text(stringResource(R.string.erro_ao_salvar, detalhe)) },
            confirmButton = { TextButton(onClick = viewModel::limparFalha) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@Composable
fun PlanoScreen(estado: PlanoEstado, acoes: AcoesPlano = AcoesPlano()) {
    val d = LocalDimensoes.current
    val titulo = (estado as? PlanoEstado.Pronto)?.moto?.apelido
        ?.let { stringResource(R.string.plano_titulo_moto, it) }
        ?: stringResource(R.string.plano_titulo)
    TelaDiario(
        titulo = titulo,
        botaoFlutuante = {
            if (estado is PlanoEstado.Pronto) {
                ExtendedFloatingActionButton(
                    onClick = acoes.onNovoItem,
                    modifier = Modifier.heightIn(min = d.toqueMinimo),
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
            is PlanoEstado.Pronto -> LazyColumn(contentPadding = padding) {
                if (estado.pendencias.isNotEmpty()) {
                    item { Secao(stringResource(R.string.plano_pendencias)) }
                    items(estado.pendencias, key = { it.id }) { PendenciaItem(it, acoes.onResolver) }
                }
                item { Secao(stringResource(R.string.plano_itens)) }
                items(estado.itens, key = { it.item.id }) { ItemPeriodico(it, acoes) }
                item {
                    Text(
                        stringResource(R.string.plano_dado_do_usuario),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(d.espaco),
                    )
                }
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
        supportingContent = { Text(stringResource(R.string.plano_pendencia_origem)) },
        trailingContent = {
            TextButton(onClick = {
                onResolver(item.id)
            }, modifier = Modifier.heightIn(min = LocalDimensoes.current.toqueMinimo)) {
                Text(stringResource(R.string.plano_resolver))
            }
        },
    )
}

@Composable
private fun ItemPeriodico(itemComSituacao: ItemComSituacao, acoes: AcoesPlano) {
    val (item, situacao) = itemComSituacao
    val proxima = listOfNotNull(
        situacao.proxima.km?.let { Formatador.km(it) },
        situacao.proxima.data?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
    ).joinToString(stringResource(R.string.plano_ou))
    val restante = listOfNotNull(
        situacao.situacao.kmRestantes?.let {
            if (it >= 0) {
                stringResource(R.string.plano_faltam, Formatador.km(it))
            } else {
                stringResource(R.string.plano_passou, Formatador.km(-it))
            }
        },
        situacao.situacao.diasRestantes?.let {
            if (it >= 0) {
                stringResource(R.string.plano_faltam, Formatador.dias(it))
            } else {
                stringResource(R.string.plano_passou, Formatador.dias(-it))
            }
        },
    ).joinToString(" · ")
    val (textoUrgencia, cor) = when (situacao.urgencia) {
        Urgencia.VENCIDO -> stringResource(R.string.plano_vencido) to MaterialTheme.colorScheme.error
        Urgencia.PROXIMO -> stringResource(R.string.plano_proximo) to MaterialTheme.colorScheme.primary
        Urgencia.EM_DIA -> stringResource(R.string.plano_em_dia) to Color.Unspecified
    }
    ListItem(
        modifier = Modifier.clickable {
            acoes.onEditarItem(item.id)
        }.heightIn(min = LocalDimensoes.current.toqueMinimo),
        overlineContent = { Text(textoUrgencia, color = cor) },
        headlineContent = { Text(item.nome) },
        supportingContent = {
            val desde = if (item.ultimoRegistroId == null) stringResource(R.string.plano_desde_cadastro) else ""
            Text(stringResource(R.string.plano_proxima, proxima) + "\n" + restante + desde)
        },
        trailingContent = {
            TextButton(
                onClick = { acoes.onRegistrarFeito(item) },
                modifier = Modifier.heightIn(min = LocalDimensoes.current.toqueMinimo),
            ) { Text(stringResource(R.string.plano_registrar_feito)) }
        },
    )
}

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
