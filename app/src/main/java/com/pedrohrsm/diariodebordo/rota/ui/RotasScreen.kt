package com.pedrohrsm.diariodebordo.rota.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.LocalDimensoes
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.TelaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.ui.BotaoPrincipal
import com.pedrohrsm.diariodebordo.core.ui.EstadoCarregando
import com.pedrohrsm.diariodebordo.core.ui.EstadoErro
import com.pedrohrsm.diariodebordo.core.ui.EstadoSemMoto
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.ui.BotaoSecundario
import com.pedrohrsm.diariodebordo.localizacao.PermissoesLocalizacao
import com.pedrohrsm.diariodebordo.numeros.Formatador
import com.pedrohrsm.diariodebordo.rota.data.Rota
import com.pedrohrsm.diariodebordo.rota.servico.GravacaoRotaService

data class AcoesRotas(
    val onGravar: () -> Unit = {},
    val onParar: () -> Unit = {},
    val onSalvarInterrompida: (String) -> Unit = {},
    val onDescartar: (String) -> Unit = {},
    val onAbrirRota: (String) -> Unit = {},
    val onCadastrarMoto: () -> Unit = {},
)

@Composable
fun RotasRoute(
    onAbrirRota: (String) -> Unit,
    onCadastrarMoto: () -> Unit,
    viewModel: RotasViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val falha by viewModel.falha.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val motoId = (estado as? RotasEstado.Pronto)?.moto?.id
    // Permissões pedidas no toque em "Gravar rota": localização e, no Android 13+, a notificação fixa.
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resposta ->
        val local = PermissoesLocalizacao.PEDIDO.any { resposta[it] == true }
        if (local && motoId != null) GravacaoRotaService.iniciar(contexto, motoId)
    }
    RotasScreen(
        estado = estado,
        acoes = AcoesRotas(
            onGravar = {
                if (motoId == null) return@AcoesRotas
                val permissoes = PermissoesLocalizacao.PEDIDO.toList() +
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        listOf(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        emptyList()
                    }
                if (PermissoesLocalizacao.temAlguma(contexto)) {
                    GravacaoRotaService.iniciar(contexto, motoId)
                } else {
                    pedir.launch(permissoes.toTypedArray())
                }
            },
            onParar = { GravacaoRotaService.parar(contexto) },
            onSalvarInterrompida = viewModel::salvarInterrompida,
            onDescartar = viewModel::descartar,
            onAbrirRota = onAbrirRota,
            onCadastrarMoto = onCadastrarMoto,
        ),
    )
    falha?.let { detalhe ->
        AlertDialog(
            onDismissRequest = viewModel::limparFalha,
            text = { Text(detalhe.ifBlank { stringResource(R.string.erro_ao_carregar) }) },
            confirmButton = { TextButton(onClick = viewModel::limparFalha) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@Composable
fun RotasScreen(estado: RotasEstado, acoes: AcoesRotas = AcoesRotas()) {
    val titulo = (estado as? RotasEstado.Pronto)?.moto?.apelido
        ?.let { stringResource(R.string.rotas_titulo_moto, it) }
        ?: stringResource(R.string.rotas_titulo)
    TelaDiario(titulo = titulo) { padding ->
        val conteudo = Modifier.fillMaxSize().padding(padding)
        when (estado) {
            RotasEstado.Carregando -> EstadoCarregando(conteudo)
            RotasEstado.SemMoto -> EstadoSemMoto(acoes.onCadastrarMoto, conteudo)
            is RotasEstado.Erro -> EstadoErro(estado.detalhe, conteudo)
            is RotasEstado.Pronto -> LazyColumn(contentPadding = padding) {
                item { PainelGravacao(estado.gravacao, acoes) }
                if (estado.rotas.isEmpty()) {
                    item { ListItem(headlineContent = { Text(stringResource(R.string.rotas_vazio)) }) }
                }
                items(estado.rotas, key = { it.id }) { rota -> RotaItem(rota) { acoes.onAbrirRota(rota.id) } }
            }
        }
    }
}

@Composable
private fun PainelGravacao(gravacao: Gravacao?, acoes: AcoesRotas) {
    val d = LocalDimensoes.current
    Column(Modifier.fillMaxWidth().padding(d.espaco), verticalArrangement = Arrangement.spacedBy(d.espacoPequeno)) {
        when {
            gravacao == null -> {
                Text(stringResource(R.string.rotas_explicacao), style = MaterialTheme.typography.bodyMedium)
                BotaoPrincipal(stringResource(R.string.rota_gravar), acoes.onGravar)
            }
            gravacao.capturando -> {
                Text(
                    stringResource(
                        R.string.rota_gravando_desde,
                        Formatador.dataHora(gravacao.rota.iniciadaEm, Formatador.fuso(gravacao.rota.fusoHorario)),
                        gravacao.pontos,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                BotaoPrincipal(stringResource(R.string.rota_parar), acoes.onParar)
            }
            else -> {
                Text(
                    stringResource(R.string.rota_interrompida, gravacao.pontos),
                    style = MaterialTheme.typography.titleMedium,
                )
                BotaoPrincipal(stringResource(R.string.rota_salvar), { acoes.onSalvarInterrompida(gravacao.rota.id) })
                BotaoSecundario(stringResource(R.string.rota_descartar), { acoes.onDescartar(gravacao.rota.id) })
            }
        }
    }
}

@Composable
private fun RotaItem(rota: Rota, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick).heightIn(min = LocalDimensoes.current.toqueMinimo),
        headlineContent = { Text(Formatador.dataHora(rota.iniciadaEm, Formatador.fuso(rota.fusoHorario))) },
        supportingContent = { Text(Formatador.distancia(rota.distanciaM) + " · " + Formatador.duracao(rota.duracaoS)) },
    )
}

@Preview
@Composable
private fun RotasPreview() {
    val moto = Moto(id = "1", apelido = "Bandida", marca = "Harley-Davidson", modelo = "883")
    val rota =
        Rota(motoId = "1", iniciadaEm = 1_790_812_800_000, distanciaM = 42_300, duracaoS = 3_900, emGravacao = false)
    DiarioTheme { RotasScreen(RotasEstado.Pronto(moto, null, listOf(rota))) }
}
