package com.pedrohrsm.diariodebordo.core.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Escolha de um dia pelo calendário do Material: um botão com [texto] (o dia já formatado) abre o seletor. Quem
 * chama converte o dia para o instante, no fuso que vale para o dado (o registro do diário, o feito do plano).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoDia(dia: LocalDate, texto: String, onDia: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var aberto by remember { mutableStateOf(false) }
    OutlinedButton(onClick = {
        aberto = true
    }, modifier = modifier.heightIn(min = LocalEscalaDiario.current.alvoMinimo)) {
        Text(texto)
    }
    if (aberto) {
        // O DatePicker trabalha com a meia-noite UTC do dia escolhido.
        val estadoData = rememberDatePickerState(
            initialSelectedDateMillis = dia.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { aberto = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoData.selectedDateMillis?.let { utc ->
                        onDia(Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    aberto = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { aberto = false }) { Text(stringResource(R.string.cancelar)) } },
        ) { DatePicker(state = estadoData) }
    }
}
