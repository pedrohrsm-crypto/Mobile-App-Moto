package com.pedrohrsm.diariodebordo.backup.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.core.ui.BotaoPrincipal
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.ui.BotaoSecundario

/** Exportar e importar pelo seletor do sistema: sem permissão de armazenamento e sem servidor. */
@Composable
fun SecaoBackup(viewModel: BackupViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val criar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(TIPO_ZIP)) { uri ->
        viewModel.exportar(uri?.toString())
    }
    val abrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.importar(uri?.toString())
    }
    SecaoBackupConteudo(
        estado = estado,
        onExportar = viewModel::pedirExportacao,
        onImportar = { abrir.launch(arrayOf(TIPO_ZIP, "application/octet-stream")) },
    )
    if (estado.confirmandoExportacao) {
        AvisoDadosPessoais(
            comRotas = estado.comRotas,
            onRotas = viewModel::alternarRotas,
            onContinuar = { criar.launch(viewModel.nomeSugerido()) },
            onCancelar = viewModel::cancelarExportacao,
        )
    }
    estado.escolhendoMoto?.let { EscolhaDaMoto(it.candidatas, viewModel::escolherMoto, viewModel::cancelarEscolha) }
    estado.resultado?.let { resultado ->
        AlertDialog(
            onDismissRequest = viewModel::fecharResultado,
            text = { Text(textoDoResultado(resultado)) },
            confirmButton = { TextButton(onClick = viewModel::fecharResultado) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@Composable
fun SecaoBackupConteudo(estado: BackupEstado, onExportar: () -> Unit = {}, onImportar: () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(LocalEscalaDiario.current.espaco(8.dp))) {
        Text(stringResource(R.string.backup_titulo), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.backup_explicacao), style = MaterialTheme.typography.bodyMedium)
        if (estado.ocupado) {
            Text(stringResource(R.string.backup_trabalhando))
            LinearProgressIndicator()
        }
        BotaoPrincipal(stringResource(R.string.backup_exportar), onExportar, habilitado = !estado.ocupado)
        Text(stringResource(R.string.backup_importar_explicacao), style = MaterialTheme.typography.bodyMedium)
        BotaoSecundario(stringResource(R.string.backup_importar), onImportar)
    }
}

/** Risco #23: o arquivo leva dado pessoal e localização; a pessoa pode deixar as rotas de fora. */
@Composable
fun AvisoDadosPessoais(
    comRotas: Boolean,
    onRotas: (Boolean) -> Unit = {},
    onContinuar: () -> Unit = {},
    onCancelar: () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.backup_aviso_titulo)) },
        text = {
            // A opção das rotas vem antes do texto: com a fonte grande, ela fica à vista sem rolar o aviso.
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    Modifier
                        .heightIn(min = LocalEscalaDiario.current.alvoMinimo)
                        .toggleable(value = comRotas, role = Role.Checkbox, onValueChange = onRotas),
                    horizontalArrangement = Arrangement.spacedBy(LocalEscalaDiario.current.espaco(8.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = comRotas, onCheckedChange = null)
                    Text(stringResource(R.string.backup_incluir_rotas))
                }
                Text(stringResource(R.string.backup_aviso_texto))
            }
        },
        confirmButton = { TextButton(onClick = onContinuar) { Text(stringResource(R.string.backup_escolher_local)) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) } },
    )
}

/** Decisão #13: um toque na moto que fica; nada é gravado antes disso. */
@Composable
fun EscolhaDaMoto(candidatas: List<Moto>, onEscolher: (String) -> Unit = {}, onCancelar: () -> Unit = {}) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.backup_escolha_titulo)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.backup_escolha_texto))
                candidatas.forEach { moto ->
                    ListItem(
                        headlineContent = { Text(moto.apelido) },
                        supportingContent = {
                            Text(stringResource(R.string.garagem_marca_modelo, moto.marca, moto.modelo))
                        },
                        modifier = Modifier
                            .heightIn(min = LocalEscalaDiario.current.alvoMinimo)
                            .clickable(role = Role.Button) { onEscolher(moto.id) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) } },
    )
}

@Composable
private fun textoDoResultado(resultado: ResultadoBackup): String = when (resultado) {
    is ResultadoBackup.Exportado -> {
        val r = resultado.resumo
        val base = stringResource(R.string.backup_exportado, r.motos, r.registros, r.fotos, r.rotas)
        if (r.fotosFaltando >
            0
        ) {
            base + "\n\n" + stringResource(R.string.backup_fotos_faltando, r.fotosFaltando)
        } else {
            base
        }
    }
    is ResultadoBackup.Importado -> {
        val r = resultado.resumo
        stringResource(R.string.backup_importado, r.novos, r.atualizados, r.fotos)
    }
    ResultadoBackup.ArquivoInvalido -> stringResource(R.string.backup_arquivo_invalido)
    ResultadoBackup.VersaoNova -> stringResource(R.string.backup_versao_nova)
    ResultadoBackup.SemEspaco -> stringResource(R.string.backup_sem_espaco)
    is ResultadoBackup.Falha -> stringResource(R.string.backup_falha, resultado.detalhe.orEmpty())
}

private const val TIPO_ZIP = "application/zip"
