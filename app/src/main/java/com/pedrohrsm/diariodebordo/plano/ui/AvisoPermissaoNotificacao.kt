package com.pedrohrsm.diariodebordo.plano.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.LocalDimensoes
import com.pedrohrsm.diariodebordo.plano.lembretes.NotificadorLembretes

/**
 * No Android 13+, notificação precisa de permissão. Pedida aqui, no plano, onde o lembrete faz sentido,
 * e nunca na abertura do app. Sem ela o plano funciona igual, só não avisa.
 */
@Composable
fun AvisoPermissaoNotificacao() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val contexto = LocalContext.current
    var permitido by remember { mutableStateOf(NotificadorLembretes.podeNotificar(contexto)) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        permitido = concedida
    }
    if (permitido) return
    ListItem(
        headlineContent = { Text(stringResource(R.string.lembrete_permissao)) },
        trailingContent = {
            TextButton(
                onClick = { pedir.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.heightIn(min = LocalDimensoes.current.toqueMinimo),
            ) { Text(stringResource(R.string.lembrete_ativar)) }
        },
    )
}
