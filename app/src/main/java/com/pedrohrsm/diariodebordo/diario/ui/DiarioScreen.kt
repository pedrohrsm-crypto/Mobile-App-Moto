package com.pedrohrsm.diariodebordo.diario.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.TelaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.core.ui.EscolhaUnica
import com.pedrohrsm.diariodebordo.core.ui.EstadoCarregando
import com.pedrohrsm.diariodebordo.core.ui.EstadoErro
import com.pedrohrsm.diariodebordo.core.ui.EstadoSemMoto
import com.pedrohrsm.diariodebordo.core.ui.EstadoVazio
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.numeros.Formatador
import kotlinx.coroutines.flow.flowOf

@Composable
fun DiarioRoute(
    onNovoRegistro: () -> Unit,
    onAbrirRegistro: (String) -> Unit,
    onCadastrarMoto: () -> Unit,
    viewModel: DiarioViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val registros = viewModel.registros.collectAsLazyPagingItems()
    DiarioScreen(estado, registros, onNovoRegistro, onAbrirRegistro, onCadastrarMoto, viewModel::filtrar)
}

@Composable
fun DiarioScreen(
    estado: DiarioEstado,
    registros: LazyPagingItems<Registro>,
    onNovoRegistro: () -> Unit = {},
    onAbrirRegistro: (String) -> Unit = {},
    onCadastrarMoto: () -> Unit = {},
    onFiltrar: (TipoRegistro?) -> Unit = {},
) {
    val escala = LocalEscalaDiario.current
    val titulo = (estado as? DiarioEstado.Pronto)?.moto?.apelido
        ?.let { stringResource(R.string.diario_titulo_moto, it) }
        ?: stringResource(R.string.diario_titulo)
    TelaDiario(
        titulo = titulo,
        botaoFlutuante = {
            if (estado is DiarioEstado.Pronto) {
                ExtendedFloatingActionButton(
                    onClick = onNovoRegistro,
                    modifier = Modifier.heightIn(min = escala.alvoMinimo),
                ) {
                    Text(stringResource(R.string.diario_novo))
                }
            }
        },
    ) { padding ->
        val conteudo = Modifier.fillMaxSize().padding(padding)
        when (estado) {
            DiarioEstado.Carregando -> EstadoCarregando(conteudo)
            DiarioEstado.SemMoto -> EstadoSemMoto(onCadastrarMoto, conteudo)
            is DiarioEstado.Erro -> EstadoErro(estado.detalhe, conteudo)
            is DiarioEstado.Pronto -> Column(conteudo) {
                EscolhaUnica(
                    opcoes = TipoRegistro.entries,
                    selecionada = estado.filtro,
                    rotulo = { rotuloTipo(it) },
                    onEscolher = onFiltrar,
                    modifier = Modifier.padding(horizontal = escala.espaco(16.dp)),
                )
                ListaDeRegistros(registros, onNovoRegistro, onAbrirRegistro)
            }
        }
    }
}

/** Três estados da lista paginada: um erro do Room aparece como erro, nunca como diário vazio. */
@Composable
private fun ListaDeRegistros(
    registros: LazyPagingItems<Registro>,
    onNovoRegistro: () -> Unit,
    onAbrirRegistro: (String) -> Unit,
) {
    when (val carga = registros.loadState.refresh) {
        is LoadState.Error -> EstadoErro(carga.error.message)
        LoadState.Loading -> if (registros.itemCount == 0) EstadoCarregando()
        is LoadState.NotLoading -> if (registros.itemCount == 0) {
            EstadoVazio(
                mensagem = stringResource(R.string.diario_vazio),
                rotuloAcao = stringResource(R.string.diario_novo),
                onAcao = onNovoRegistro,
            )
        }
    }
    if (registros.itemCount > 0) {
        LazyColumn {
            items(registros.itemCount, key = registros.itemKey { it.id }) { indice ->
                registros[indice]?.let { registro ->
                    RegistroItem(registro, onClick = { onAbrirRegistro(registro.id) })
                }
            }
        }
    }
}

@Composable
private fun RegistroItem(registro: Registro, onClick: () -> Unit) {
    val detalhes = listOfNotNull(
        Formatador.data(registro.dataEm, Formatador.fuso(registro.fusoHorario)),
        registro.hodometroKm?.let { Formatador.km(it) },
        registro.valorCentavos?.let { Formatador.dinheiro(it) },
        registro.litrosMl?.let { Formatador.litros(it) },
    ).joinToString(" · ")
    ListItem(
        modifier = Modifier.clickable(onClick = onClick).heightIn(min = LocalEscalaDiario.current.alvoMinimo),
        overlineContent = { Text(rotuloTipo(registro.tipo)) },
        headlineContent = { Text(registro.titulo) },
        supportingContent = { Text(detalhes) },
    )
}

@Composable
fun rotuloTipo(tipo: TipoRegistro): String = stringResource(
    when (tipo) {
        TipoRegistro.MANUTENCAO -> R.string.tipo_manutencao
        TipoRegistro.ABASTECIMENTO -> R.string.tipo_abastecimento
        TipoRegistro.OCORRENCIA -> R.string.tipo_ocorrencia
        TipoRegistro.ROLE -> R.string.tipo_role
        TipoRegistro.NOTA -> R.string.tipo_nota
    },
)

@Preview
@Composable
private fun DiarioPreview() {
    val moto = Moto(id = "1", apelido = "Bandida", marca = "Harley-Davidson", modelo = "883")
    val registro = Registro(
        motoId = "1",
        tipo = TipoRegistro.ABASTECIMENTO,
        titulo = "Posto da estrada",
        dataEm = 1_790_812_800_000,
        hodometroKm = 23_450,
        valorCentavos = 8_990,
        litrosMl = 12_500,
    )
    val registros = flowOf(PagingData.from(listOf(registro))).collectAsLazyPagingItems()
    DiarioTheme { DiarioScreen(DiarioEstado.Pronto(moto, null), registros) }
}
