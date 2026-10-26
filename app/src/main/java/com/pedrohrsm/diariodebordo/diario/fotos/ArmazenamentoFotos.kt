package com.pedrohrsm.diariodebordo.diario.fotos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.pedrohrsm.diariodebordo.diario.dominio.escalaFinal
import com.pedrohrsm.diariodebordo.diario.dominio.fatorDeReducao
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A foto é aberta já reduzida, girada conforme a orientação da câmera e gravada de novo em JPEG.
 * Regravar descarta todo o EXIF da original, inclusive a posição de GPS (docs/PLANEJAMENTO.md, Segurança).
 */
@Singleton
class ArmazenamentoFotos @Inject constructor(@ApplicationContext private val context: Context) : GuardaFotos {

    private val pasta: File get() = File(context.filesDir, PASTA).apply { mkdirs() }

    fun arquivo(nome: String): File = File(pasta, nome)

    override suspend fun importar(uri: String): String = withContext(Dispatchers.IO) {
        val origem = Uri.parse(uri)
        val resolver = context.contentResolver
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(origem)?.use { BitmapFactory.decodeStream(it, null, limites) }
            ?: throw IOException("Foto indisponível")
        if (limites.outWidth <= 0 || limites.outHeight <= 0) throw IOException("Arquivo não é uma imagem")

        val opcoes = BitmapFactory.Options().apply {
            inSampleSize = fatorDeReducao(limites.outWidth, limites.outHeight)
        }
        val reduzida = resolver.openInputStream(origem)?.use { BitmapFactory.decodeStream(it, null, opcoes) }
            ?: throw IOException("Não foi possível abrir a foto")
        val rotacao = resolver.openInputStream(origem)?.use { rotacaoExif(ExifInterface(it)) } ?: 0f

        val escala = escalaFinal(reduzida.width, reduzida.height).toFloat()
        val matriz = Matrix().apply {
            postScale(escala, escala)
            postRotate(rotacao)
        }
        val final = Bitmap.createBitmap(reduzida, 0, 0, reduzida.width, reduzida.height, matriz, true)

        val nome = "${UUID.randomUUID()}.jpg"
        val destino = arquivo(nome)
        try {
            destino.outputStream().use { saida ->
                check(final.compress(Bitmap.CompressFormat.JPEG, QUALIDADE, saida)) { "Falha ao comprimir a foto" }
            }
        } catch (e: Exception) {
            destino.delete()
            throw e
        } finally {
            if (final !== reduzida) final.recycle()
            reduzida.recycle()
        }
        nome
    }

    override fun apagar(arquivo: String): Boolean {
        val alvo = arquivo(arquivo)
        return !alvo.exists() || alvo.delete()
    }

    private fun rotacaoExif(exif: ExifInterface): Float =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

    companion object {
        const val PASTA = "fotos"
        private const val QUALIDADE = 80
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FotosModule {
    @Binds
    abstract fun bindGuardaFotos(impl: ArmazenamentoFotos): GuardaFotos
}
