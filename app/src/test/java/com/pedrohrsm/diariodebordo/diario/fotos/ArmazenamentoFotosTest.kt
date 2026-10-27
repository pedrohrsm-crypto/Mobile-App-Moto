package com.pedrohrsm.diariodebordo.diario.fotos

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Risco #23: a foto do diário mostra onde a pessoa estava. A foto de origem tem posição de GPS no EXIF;
 * a guardada no armazenamento interno não pode ter nenhuma tag de GPS.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ArmazenamentoFotosTest {

    private val contexto: Application = RuntimeEnvironment.getApplication()
    private val armazenamento = ArmazenamentoFotos(contexto)
    private lateinit var origem: File

    @Before
    fun fotoDaCameraComGps() {
        origem = File(contexto.cacheDir, "camera.jpg")
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888).also { Canvas(it).drawRGB(200, 120, 40) }
        origem.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        ExifInterface(origem.path).apply {
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "23/1,33/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "46/1,38/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W")
            setAttribute(ExifInterface.TAG_GPS_ALTITUDE, "760/1")
            setAttribute(ExifInterface.TAG_GPS_DATESTAMP, "2026:10:01")
            setAttribute(ExifInterface.TAG_MAKE, "Camera de teste")
            saveAttributes()
        }
        // O teste só vale se a origem tem mesmo o GPS.
        assertTrue(ExifInterface(origem.path).temPosicao())
    }

    @Test
    fun fotoGuardadaNaoTemNenhumaTagDeGps() = runTest {
        val nome = armazenamento.importar(Uri.fromFile(origem).toString())
        val guardada = ExifInterface(armazenamento.arquivo(nome).path)
        assertFalse(guardada.temPosicao())
        TAGS_DE_GPS.forEach { tag -> assertNull("$tag ficou na foto", guardada.getAttribute(tag)) }
        assertNull(guardada.getAttribute(ExifInterface.TAG_MAKE))
    }

    @Test
    fun fotoGuardadaFicaNoArmazenamentoInternoComOTamanhoDaOrigem() = runTest {
        val nome = armazenamento.importar(Uri.fromFile(origem).toString())
        val guardada = armazenamento.arquivo(nome)
        assertEquals(File(contexto.filesDir, ArmazenamentoFotos.PASTA), guardada.parentFile)
        val exif = ExifInterface(guardada.path)
        assertEquals(640, exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0))
        assertEquals(480, exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0))
    }

    private fun ExifInterface.temPosicao(): Boolean = getLatLong(FloatArray(2))

    private companion object {
        val TAGS_DE_GPS = listOf(
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_DATESTAMP,
        )
    }
}
