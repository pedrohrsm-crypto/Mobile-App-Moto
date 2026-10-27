package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.FormularioDiario
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.TelaDiario
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.core.ui.BotaoPrincipal
import com.pedrohrsm.diariodebordo.core.ui.CampoTexto
import com.pedrohrsm.diariodebordo.core.ui.EscolhaUnica
import com.pedrohrsm.diariodebordo.core.ui.EstadoCarregando
import com.pedrohrsm.diariodebordo.garagem.data.CategoriaMoto
import com.pedrohrsm.diariodebordo.garagem.dominio.CampoMoto

@Composable
fun MotoFormRoute(onSalvo: (String) -> Unit, viewModel: MotoFormViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.salvoId) { estado.salvoId?.let(onSalvo) }
    MotoFormScreen(estado = estado, onAlterar = viewModel::alterar, onSalvar = viewModel::salvar)
}

@Composable
fun MotoFormScreen(
    estado: MotoFormEstado,
    onAlterar: ((MotoFormEstado) -> MotoFormEstado) -> Unit = {},
    onSalvar: () -> Unit = {},
) {
    val titulo = stringResource(if (estado.editando) R.string.moto_form_editar else R.string.moto_form_nova)
    TelaDiario(titulo = titulo) { padding ->
        if (estado.carregando) {
            EstadoCarregando(Modifier.fillMaxSize().padding(padding))
        } else {
            MotoFormulario(estado, padding, onAlterar, onSalvar)
        }
    }
}

@Composable
private fun MotoFormulario(
    estado: MotoFormEstado,
    padding: PaddingValues,
    onAlterar: ((MotoFormEstado) -> MotoFormEstado) -> Unit,
    onSalvar: () -> Unit,
) {
    val obrigatorio = stringResource(R.string.campo_obrigatorio)
    FormularioDiario(padding) {
        Column(verticalArrangement = Arrangement.spacedBy(LocalEscalaDiario.current.espaco(8.dp))) {
            CampoTexto(
                valor = estado.apelido,
                onValor = { v -> onAlterar { it.copy(apelido = v) } },
                rotulo = stringResource(R.string.moto_apelido),
                erro = obrigatorio.takeIf { CampoMoto.APELIDO in estado.invalidos },
            )
            CampoTexto(
                valor = estado.marca,
                onValor = { v -> onAlterar { it.copy(marca = v) } },
                rotulo = stringResource(R.string.moto_marca),
                erro = obrigatorio.takeIf { CampoMoto.MARCA in estado.invalidos },
            )
            CampoTexto(
                valor = estado.modelo,
                onValor = { v -> onAlterar { it.copy(modelo = v) } },
                rotulo = stringResource(R.string.moto_modelo),
                erro = obrigatorio.takeIf { CampoMoto.MODELO in estado.invalidos },
            )
            CampoTexto(
                valor = estado.ano,
                onValor = { v -> onAlterar { it.copy(ano = v) } },
                rotulo = stringResource(R.string.moto_ano),
                teclado = KeyboardType.Number,
                erro = stringResource(R.string.moto_ano_invalido).takeIf { CampoMoto.ANO in estado.invalidos },
            )
            if (!estado.editando) {
                CampoTexto(
                    valor = estado.kmInicial,
                    onValor = { v -> onAlterar { it.copy(kmInicial = v) } },
                    rotulo = stringResource(R.string.moto_km_inicial),
                    teclado = KeyboardType.Number,
                    erro = stringResource(R.string.km_invalido).takeIf { CampoMoto.KM in estado.invalidos },
                )
            }
            Text(stringResource(R.string.moto_categoria), style = MaterialTheme.typography.labelLarge)
            EscolhaUnica(
                opcoes = CategoriaMoto.entries,
                selecionada = estado.categoria,
                rotulo = { rotuloCategoria(it) },
                onEscolher = { c -> onAlterar { it.copy(categoria = c) } },
            )
            estado.erro?.let { erro ->
                Text(
                    text = when (erro) {
                        ErroMotoForm.NaoEncontrada -> stringResource(R.string.moto_nao_encontrada)
                        is ErroMotoForm.LimiteGratis -> stringResource(R.string.garagem_limite_gratis)
                        is ErroMotoForm.Falha -> stringResource(R.string.erro_ao_salvar, erro.detalhe.orEmpty())
                    },
                    color = MaterialTheme.colorScheme.error,
                )
            }
            BotaoPrincipal(
                texto = stringResource(R.string.salvar),
                onClick = onSalvar,
                habilitado = !estado.salvando,
            )
        }
    }
}

@Composable
fun rotuloCategoria(categoria: CategoriaMoto): String = stringResource(
    when (categoria) {
        CategoriaMoto.CUSTOM -> R.string.categoria_custom
        CategoriaMoto.STREET -> R.string.categoria_street
        CategoriaMoto.TRAIL -> R.string.categoria_trail
        CategoriaMoto.SCOOTER -> R.string.categoria_scooter
    },
)

@Preview
@Composable
private fun MotoFormPreview() {
    DiarioTheme { MotoFormScreen(estado = MotoFormEstado(invalidos = setOf(CampoMoto.APELIDO))) }
}
