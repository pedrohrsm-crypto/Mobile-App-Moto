package com.pedrohrsm.diariodebordo.diario.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.pedrohrsm.diariodebordo.diario.fotos.ArmazenamentoFotos
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Três estados da miniatura: abrindo, aberta e ilegível (arquivo apagado ou corrompido). */
private sealed interface Miniatura {
    data object Abrindo : Miniatura
    data class Aberta(val imagem: ImageBitmap) : Miniatura
    data object Ilegivel : Miniatura
}

/** Miniatura de uma foto do diário, aberta reduzida fora da thread principal. */
@Composable
fun FotoMiniatura(arquivo: String, descricao: String, tamanho: Dp, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val miniatura by produceState<Miniatura>(Miniatura.Abrindo, arquivo) {
        value = withContext(Dispatchers.IO) {
            val caminho = File(File(contexto.filesDir, ArmazenamentoFotos.PASTA), arquivo).path
            BitmapFactory.decodeFile(caminho, BitmapFactory.Options().apply { inSampleSize = 4 })
                ?.let { Miniatura.Aberta(it.asImageBitmap()) }
                ?: Miniatura.Ilegivel
        }
    }
    Box(modifier.size(tamanho), contentAlignment = Alignment.Center) {
        when (val atual = miniatura) {
            Miniatura.Abrindo -> CircularProgressIndicator()
            is Miniatura.Aberta -> Image(
                bitmap = atual.imagem,
                contentDescription = descricao,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(tamanho),
            )
            // Sem a imagem, fica o quadro vazio com a descrição para o leitor de tela; o botão Remover segue ao lado.
            Miniatura.Ilegivel -> Box(
                Modifier
                    .size(tamanho)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .semantics { contentDescription = descricao },
            )
        }
    }
}
