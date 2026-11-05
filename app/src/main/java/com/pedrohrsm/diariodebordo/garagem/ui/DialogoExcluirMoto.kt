package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.core.ui.CampoTexto

const val TAG_BOTAO_EXCLUIR_MOTO = "botao_excluir_moto"

/**
 * Ação destrutiva no fim da tela da moto, longe do hodômetro e do diário (#65, UX/UI). Cor padrão do TextButton:
 * a guarda de contraste (#55) só aceita as cores que ela mede; o texto e a confirmação dizem que é destrutivo.
 */
@Composable
fun BotaoExcluirMoto(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = LocalEscalaDiario.current.alvoMinimo),
    ) { Text(stringResource(R.string.moto_excluir)) }
}

/**
 * Confirmação da exclusão: diz o que some, com a contagem real, sugere o backup antes e só libera o botão
 * quando a pessoa digita o apelido da moto. O texto rola: em fonte 200% com o teclado aberto ele não cabe.
 */
@Composable
fun DialogoExcluirMoto(
    dialogo: DialogoExclusao,
    onTexto: (String) -> Unit,
    onExcluir: () -> Unit,
    onBackup: () -> Unit,
    onFechar: () -> Unit,
) {
    val escala = LocalEscalaDiario.current
    AlertDialog(
        onDismissRequest = { if (!dialogo.excluindo) onFechar() },
        title = { Text(stringResource(R.string.moto_excluir_titulo, dialogo.apelido)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(escala.espaco(8.dp)),
            ) {
                val contagem = dialogo.contagem
                if (contagem == null) {
                    Text(stringResource(R.string.moto_excluir_contando))
                } else {
                    Text(stringResource(R.string.moto_excluir_some))
                    Text(pluralStringResource(R.plurals.moto_excluir_registros, contagem.registros, contagem.registros))
                    Text(pluralStringResource(R.plurals.moto_excluir_fotos, contagem.fotos, contagem.fotos))
                    Text(
                        pluralStringResource(
                            R.plurals.moto_excluir_itens,
                            contagem.itensDoPlano,
                            contagem.itensDoPlano,
                        ),
                    )
                    Text(pluralStringResource(R.plurals.moto_excluir_rotas, contagem.rotas, contagem.rotas))
                    Text(pluralStringResource(R.plurals.moto_excluir_leituras, contagem.leituras, contagem.leituras))
                }
                Text(stringResource(R.string.moto_excluir_backup))
                TextButton(onClick = onBackup, enabled = !dialogo.excluindo) {
                    Text(stringResource(R.string.moto_excluir_ir_backup))
                }
                // A instrução fica fora do campo: um rótulo que quebra linha dentro do OutlinedTextField (fonte 200%
                // no modo oficina) mede o campo sem parar e trava o diálogo. O rótulo é curto e cabe sempre.
                Text(stringResource(R.string.moto_excluir_digite, dialogo.apelido))
                CampoTexto(
                    valor = dialogo.texto,
                    onValor = onTexto,
                    rotulo = stringResource(R.string.moto_apelido),
                )
                val erro = when (val e = dialogo.erro) {
                    null -> null
                    ErroExclusao.Gravando -> stringResource(R.string.moto_excluir_gravando)
                    is ErroExclusao.Falha -> stringResource(R.string.moto_excluir_falha, e.detalhe.orEmpty())
                }
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onExcluir,
                enabled = dialogo.podeExcluir,
                modifier = Modifier.testTag(TAG_BOTAO_EXCLUIR_MOTO),
            ) { Text(stringResource(R.string.excluir)) }
        },
        dismissButton = {
            TextButton(onClick = onFechar, enabled = !dialogo.excluindo) { Text(stringResource(R.string.cancelar)) }
        },
    )
}

/**
 * A moto já foi excluída, mas um passo depois do commit falhou (#72): diz o que ficou para trás. Ao fechar, a tela
 * volta para a garagem.
 */
@Composable
fun DialogoAvisoExclusao(aviso: AvisoExclusao, onFechar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(stringResource(R.string.moto_excluida_titulo)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(LocalEscalaDiario.current.espaco(8.dp)),
            ) {
                if (aviso.fotosQueFicaram > 0) {
                    Text(
                        pluralStringResource(
                            R.plurals.moto_excluida_fotos_ficaram,
                            aviso.fotosQueFicaram,
                            aviso.fotosQueFicaram,
                        ),
                    )
                }
                if (!aviso.lembretesRecalculados) Text(stringResource(R.string.moto_excluida_lembretes))
            }
        },
        confirmButton = { TextButton(onClick = onFechar) { Text(stringResource(R.string.ok)) } },
    )
}
