package com.pedrohrsm.diariodebordo.core.designsystem.capturas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTema
import com.pedrohrsm.diariodebordo.core.designsystem.theme.PapelCor

/** Só para as capturas: mostra os 17 papéis do tema ativo, cada um com o seu quadrado e o nome do papel. */
@Composable
fun PaletaDeTokens() {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PapelCor.entries.forEach { papel ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(DiarioTema.cor(papel))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                )
                Text(
                    text = papel.name.lowercase(),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
