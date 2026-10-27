package com.pedrohrsm.diariodebordo.rota.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pedrohrsm.diariodebordo.rota.dominio.Polilinha
import com.pedrohrsm.diariodebordo.rota.dominio.projetar

/**
 * Traçado da rota sobre fundo neutro, sem mapa (plano de ação, 1.7): funciona sem internet.
 * Início marcado com um círculo cheio e fim com um vazado.
 */
@Composable
fun TracadoRota(polilinha: String, descricao: String, modifier: Modifier = Modifier) {
    val pontos = remember(polilinha) { runCatching { Polilinha.decodificar(polilinha) }.getOrDefault(emptyList()) }
    val cor = MaterialTheme.colorScheme.primary
    val borda = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(1.dp, borda, MaterialTheme.shapes.medium)
            .semantics { contentDescription = descricao },
    ) {
        val margem = 16.dp.toPx()
        val xy = projetar(pontos, size.width, size.height, margem)
        if (xy.isEmpty()) return@Canvas
        val traco = 4.dp.toPx()
        if (xy.size > 1) {
            val caminho = Path().apply {
                moveTo(xy[0].first, xy[0].second)
                xy.drop(1).forEach { (x, y) -> lineTo(x, y) }
            }
            drawPath(caminho, cor, style = Stroke(width = traco, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        drawCircle(cor, radius = traco * 1.6f, center = Offset(xy.first().first, xy.first().second))
        drawCircle(
            cor,
            radius = traco * 1.6f,
            center = Offset(xy.last().first, xy.last().second),
            style = Stroke(width = traco / 1.5f),
        )
    }
}
