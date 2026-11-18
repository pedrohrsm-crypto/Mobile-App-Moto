package com.pedrohrsm.diariodebordo.rota.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pedrohrsm.diariodebordo.rota.dominio.Trecho
import com.pedrohrsm.diariodebordo.rota.dominio.projetar

/**
 * Traçado da rota sobre fundo neutro, sem mapa (plano de ação, 1.7): funciona sem internet.
 * Início marcado com um círculo cheio e fim com um vazado. Recebe os trechos já divididos: uma polilinha
 * corrompida vira erro no [RotaDetalheViewModel], nunca um quadro vazio. Traço visível em `primary`; o trecho que
 * sai oculto no arquivo exportado (#78) em `outline` e tracejado, para não depender só da cor. Moldura em
 * `outline`. As duas cores têm 3:1 sobre o fundo (ContrasteTemaTest).
 */
@Composable
fun TracadoRota(trechos: List<Trecho>, descricao: String, modifier: Modifier = Modifier) {
    val cor = MaterialTheme.colorScheme.primary
    val cinza = MaterialTheme.colorScheme.outline
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(1.dp, cinza, MaterialTheme.shapes.medium)
            .semantics { contentDescription = descricao },
    ) {
        val margem = 16.dp.toPx()
        val todos = trechos.flatMap { it.pontos }
        val xy = projetar(todos, size.width, size.height, margem)
        if (xy.isEmpty()) return@Canvas
        val traco = 4.dp.toPx()
        var i = 0
        trechos.forEach { trecho ->
            val parte = xy.subList(i, i + trecho.pontos.size)
            i += trecho.pontos.size
            if (parte.size > 1) {
                val caminho = Path().apply {
                    moveTo(parte[0].first, parte[0].second)
                    parte.drop(1).forEach { (x, y) -> lineTo(x, y) }
                }
                val estilo = if (trecho.oculto) {
                    Stroke(
                        width = traco * 0.75f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(traco * 2, traco * 1.5f)),
                    )
                } else {
                    Stroke(width = traco, cap = StrokeCap.Round, join = StrokeJoin.Round)
                }
                drawPath(caminho, if (trecho.oculto) cinza else cor, style = estilo)
            }
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
