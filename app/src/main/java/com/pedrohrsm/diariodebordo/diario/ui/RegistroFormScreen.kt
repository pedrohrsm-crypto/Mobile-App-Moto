package com.pedrohrsm.diariodebordo.diario.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.pedrohrsm.diariodebordo.diario.data.TipoOcorrencia
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.diario.dominio.CampoRegistro
import com.pedrohrsm.diariodebordo.diario.dominio.MAXIMO_FOTOS
import com.pedrohrsm.diariodebordo.numeros.Formatador
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
fun RegistroFormRoute(onConcluido: () -> Unit, viewModel: RegistroFormViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.concluido) { if (estado.concluido) onConcluido() }
    val escolherFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.adicionarFoto(it.toString()) }
    }
    RegistroFormScreen(
        estado = estado,
        acoes = AcoesRegistroForm(
            onAlterar = viewModel::alterar,
            onSalvar = viewModel::salvar,
            onExcluir = viewModel::excluir,
            onAdicionarFoto = {
                escolherFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemoverFoto = viewModel::removerFoto,
        ),
    )
}

data class AcoesRegistroForm(
    val onAlterar: ((RegistroFormEstado) -> RegistroFormEstado) -> Unit = {},
    val onSalvar: () -> Unit = {},
    val onExcluir: () -> Unit = {},
    val onAdicionarFoto: () -> Unit = {},
    val onRemoverFoto: (String) -> Unit = {},
)

@Composable
fun RegistroFormScreen(estado: RegistroFormEstado, acoes: AcoesRegistroForm = AcoesRegistroForm()) {
    var confirmarExclusao by remember { mutableStateOf(false) }
    TelaDiario(
        titulo = stringResource(if (estado.editando) R.string.registro_editar else R.string.registro_novo),
        acoes = {
            if (estado.editando && !estado.carregando) {
                TextButton(onClick = { confirmarExclusao = true }) { Text(stringResource(R.string.excluir)) }
            }
            // Salvar também no topo: sem rolar a tela, o que conta no modo oficina e com o teclado aberto.
            if (!estado.carregando && estado.motoId != null) {
                TextButton(
                    onClick = acoes.onSalvar,
                    enabled = !estado.salvando,
                    modifier = Modifier.heightIn(min = LocalEscalaDiario.current.alvoMinimo),
                ) { Text(stringResource(R.string.salvar)) }
            }
        },
    ) { padding ->
        if (estado.carregando) {
            EstadoCarregando(Modifier.fillMaxSize().padding(padding))
        } else {
            RegistroFormulario(estado, padding, acoes)
        }
    }
    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text(stringResource(R.string.registro_excluir_titulo)) },
            text = { Text(stringResource(R.string.registro_excluir_texto)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarExclusao = false
                    acoes.onExcluir()
                }) { Text(stringResource(R.string.excluir)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusao = false }) { Text(stringResource(R.string.cancelar)) }
            },
        )
    }
}

@Composable
private fun RegistroFormulario(estado: RegistroFormEstado, padding: PaddingValues, acoes: AcoesRegistroForm) {
    val escala = LocalEscalaDiario.current
    val onAlterar = acoes.onAlterar
    val rotulos = TipoRegistro.entries.associateWith { rotuloTipo(it) }
    LaunchedEffect(Unit) {
        if (!estado.editando && estado.titulo.isBlank()) onAlterar { it.copy(titulo = rotulos.getValue(it.tipo)) }
    }
    FormularioDiario(padding) {
        Column(verticalArrangement = Arrangement.spacedBy(escala.espaco(8.dp))) {
            EscolhaUnica(
                opcoes = TipoRegistro.entries,
                selecionada = estado.tipo,
                rotulo = { rotulos.getValue(it) },
                permiteNenhuma = false,
                onEscolher = { tipo ->
                    if (tipo != null) {
                        onAlterar { e ->
                            // Título ainda automático acompanha o tipo; título digitado fica.
                            val automatico = e.titulo.isBlank() || e.titulo == rotulos[e.tipo]
                            e.copy(tipo = tipo, titulo = if (automatico) rotulos.getValue(tipo) else e.titulo)
                        }
                    }
                },
            )
            CampoTexto(
                valor = estado.titulo,
                onValor = { v -> onAlterar { it.copy(titulo = v) } },
                rotulo = stringResource(R.string.registro_titulo),
                erro = stringResource(R.string.campo_obrigatorio).takeIf { CampoRegistro.TITULO in estado.invalidos },
            )
            CampoData(estado.dataEm, onData = { millis -> onAlterar { it.copy(dataEm = millis) } })
            CampoTexto(
                valor = estado.km,
                onValor = { v -> onAlterar { it.copy(km = v) } },
                rotulo = stringResource(R.string.registro_km),
                teclado = KeyboardType.Number,
                erro = stringResource(R.string.km_invalido).takeIf { CampoRegistro.KM in estado.invalidos },
            )
            CampoTexto(
                valor = estado.valor,
                onValor = { v -> onAlterar { it.copy(valor = v) } },
                rotulo = stringResource(R.string.registro_valor),
                teclado = KeyboardType.Decimal,
                erro = stringResource(R.string.registro_valor_invalido).takeIf {
                    CampoRegistro.VALOR in estado.invalidos
                },
            )
            if (estado.tipo == TipoRegistro.ABASTECIMENTO) {
                CampoTexto(
                    valor = estado.litros,
                    onValor = { v -> onAlterar { it.copy(litros = v) } },
                    rotulo = stringResource(R.string.registro_litros),
                    teclado = KeyboardType.Decimal,
                    erro = stringResource(R.string.registro_litros_invalido)
                        .takeIf { CampoRegistro.LITROS in estado.invalidos },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = estado.tanqueCheio,
                        onCheckedChange = { v -> onAlterar { it.copy(tanqueCheio = v) } },
                    )
                    Text(stringResource(R.string.registro_tanque_cheio))
                }
            }
            if (estado.tipo == TipoRegistro.OCORRENCIA) {
                Text(stringResource(R.string.registro_ocorrencia), style = MaterialTheme.typography.labelLarge)
                EscolhaUnica(
                    opcoes = TipoOcorrencia.entries,
                    selecionada = estado.ocorrencia,
                    rotulo = { rotuloOcorrencia(it) },
                    onEscolher = { o -> onAlterar { it.copy(ocorrencia = o) } },
                )
            }
            CampoTexto(
                valor = estado.descricao,
                onValor = { v -> onAlterar { it.copy(descricao = v) } },
                rotulo = stringResource(R.string.registro_descricao),
                linhas = 3,
            )
            Fotos(estado, acoes)
            estado.erro?.let { Text(textoDoErro(it), color = MaterialTheme.colorScheme.error) }
            BotaoPrincipal(
                texto = stringResource(R.string.salvar),
                onClick = acoes.onSalvar,
                habilitado = !estado.salvando && estado.motoId != null,
            )
        }
    }
}

@Composable
private fun Fotos(estado: RegistroFormEstado, acoes: AcoesRegistroForm) {
    val escala = LocalEscalaDiario.current
    Text(
        stringResource(R.string.registro_fotos, estado.fotos.size, MAXIMO_FOTOS),
        style = MaterialTheme.typography.labelLarge,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(escala.espaco(8.dp))) {
        estado.fotos.forEachIndexed { i, arquivo ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FotoMiniatura(arquivo, stringResource(R.string.registro_foto_descricao, i + 1), 96.dp)
                TextButton(onClick = {
                    acoes.onRemoverFoto(arquivo)
                }, modifier = Modifier.heightIn(min = escala.alvoMinimo)) {
                    Text(stringResource(R.string.remover))
                }
            }
        }
    }
    if (estado.podeAdicionarFoto) {
        OutlinedButton(onClick = acoes.onAdicionarFoto, modifier = Modifier.heightIn(min = escala.alvoMinimo)) {
            Text(stringResource(R.string.registro_adicionar_foto))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoData(dataEm: Long, onData: (Long) -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    val fuso = ZoneId.systemDefault()
    OutlinedButton(onClick = {
        aberto = true
    }, modifier = Modifier.heightIn(min = LocalEscalaDiario.current.alvoMinimo)) {
        Text(stringResource(R.string.registro_data, Formatador.data(dataEm, fuso)))
    }
    if (aberto) {
        // O DatePicker trabalha com a meia-noite UTC do dia escolhido; a hora do registro é mantida.
        val dia = Instant.ofEpochMilli(dataEm).atZone(fuso)
        val estadoData = rememberDatePickerState(
            initialSelectedDateMillis = dia.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { aberto = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoData.selectedDateMillis?.let { utc ->
                        val escolhido = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                        onData(juntar(escolhido, dia.toLocalTime(), fuso))
                    }
                    aberto = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { aberto = false }) { Text(stringResource(R.string.cancelar)) } },
        ) { DatePicker(state = estadoData) }
    }
}

private fun juntar(data: LocalDate, hora: LocalTime, fuso: ZoneId): Long =
    data.atTime(hora).atZone(fuso).toInstant().toEpochMilli()

@Composable
fun rotuloOcorrencia(tipo: TipoOcorrencia): String = stringResource(
    when (tipo) {
        TipoOcorrencia.QUEDA -> R.string.ocorrencia_queda
        TipoOcorrencia.BARULHO -> R.string.ocorrencia_barulho
        TipoOcorrencia.VAZAMENTO -> R.string.ocorrencia_vazamento
        TipoOcorrencia.OUTRA -> R.string.ocorrencia_outra
    },
)

@Composable
private fun textoDoErro(erro: ErroRegistroForm): String = when (erro) {
    ErroRegistroForm.SemMoto -> stringResource(R.string.sem_moto_mensagem)
    ErroRegistroForm.NaoEncontrado -> stringResource(R.string.registro_nao_encontrado)
    is ErroRegistroForm.FalhaFoto -> stringResource(R.string.registro_falha_foto, erro.detalhe.orEmpty())
    is ErroRegistroForm.Falha -> stringResource(R.string.erro_ao_salvar, erro.detalhe.orEmpty())
}

@Preview
@Composable
private fun RegistroFormPreview() {
    DiarioTheme {
        RegistroFormScreen(
            RegistroFormEstado(
                carregando = false,
                motoId = "1",
                tipo = TipoRegistro.ABASTECIMENTO,
                titulo = "Abastecimento",
                dataEm = 1_790_812_800_000,
            ),
        )
    }
}
