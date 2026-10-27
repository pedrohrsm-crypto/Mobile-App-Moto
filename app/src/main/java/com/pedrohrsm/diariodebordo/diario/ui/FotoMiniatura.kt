package com.pedrohrsm.diariodebordo.diario.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.pedrohrsm.diariodebordo.diario.fotos.ArmazenamentoFotos
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Miniatura de uma foto do diário, aberta reduzida fora da thread principal. */
@Composable
fun FotoMiniatura(arquivo: String, descricao: String, tamanho: Dp, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val imagem by produceState<ImageBitmap?>(null, arquivo) {
        value = withContext(Dispatchers.IO) {
            val caminho = File(File(contexto.filesDir, ArmazenamentoFotos.PASTA), arquivo).path
            BitmapFactory.decodeFile(caminho, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap()
        }
    }
    Box(modifier.size(tamanho), contentAlignment = Alignment.Center) {
        val atual = imagem
        if (atual == null) {
            CircularProgressIndicator()
        } else {
            Image(
                bitmap = atual,
                contentDescription = descricao,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(tamanho),
            )
        }
    }
}
