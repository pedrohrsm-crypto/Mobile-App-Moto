package com.pedrohrsm.diariodebordo.core.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.pedrohrsm.diariodebordo.R
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Guarda do ícone do launcher (#84). A zona segura do ícone adaptativo é o círculo de 66 dp no centro dos 108 dp:
 * fora dele a máscara redonda do fabricante corta o guidão. Medir só uma caixa quadrada deixa passar os cantos (a
 * ponta da manopla fica a ~38 dp do centro com a caixa dentro de 66 dp), então o teste desenha o vetor de verdade
 * (4 px por dp) e mede a distância de cada pixel pintado até o centro.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class IconeLauncherTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun simboloDoLauncherCabeNoCirculoDe66DpDaZonaSegura() {
        val lado = TAMANHO_DP * PX_POR_DP
        val centro = lado / 2.0
        val limite = ZONA_SEGURA_DP / 2.0 * PX_POR_DP
        val distanciaMaxima = pixelsPintados().maxOf { (x, y) -> hypot(x + 0.5 - centro, y + 0.5 - centro) }
        assertTrue(
            "pixel mais distante a ${distanciaMaxima / PX_POR_DP} dp do centro; o limite é ${limite / PX_POR_DP} dp",
            distanciaMaxima <= limite,
        )
    }

    @Test
    fun simboloDoLauncherFicaCentralizadoNaHorizontalEOcupaAZonaSegura() {
        val pintados = pixelsPintados()
        val esquerda = pintados.minOf { it.first }
        val direita = pintados.maxOf { it.first } + 1
        val altura = pintados.maxOf { it.second } + 1 - pintados.minOf { it.second }
        assertEquals("centro horizontal", TAMANHO_DP * PX_POR_DP / 2.0, (esquerda + direita) / 2.0, 2.0)
        // Símbolo pequeno demais some em 48 dp: pede ao menos 80% da altura da zona segura.
        assertTrue("altura $altura px", altura >= ZONA_SEGURA_DP * PX_POR_DP * ALTURA_MINIMA)
    }

    @Test
    fun fundoDoIconeAdaptativoEOGrafiteDaIdentidadeVisual() {
        assertEquals(0xFF1A1C1E.toInt(), contexto.getColor(R.color.ic_launcher_background))
    }

    private fun pixelsPintados(): List<Pair<Int, Int>> {
        val lado = TAMANHO_DP * PX_POR_DP
        val bitmap = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        val vetor = checkNotNull(ContextCompat.getDrawable(contexto, R.drawable.ic_launcher_foreground))
        vetor.setBounds(0, 0, lado, lado)
        vetor.draw(Canvas(bitmap))
        val pintados = buildList {
            for (y in 0 until lado) {
                for (x in 0 until lado) {
                    if (bitmap.getPixel(x, y) ushr 24 > LIMITE_ALFA) add(x to y)
                }
            }
        }
        assertTrue("o vetor não pintou nada", pintados.isNotEmpty())
        return pintados
    }

    private companion object {
        const val TAMANHO_DP = 108
        const val ZONA_SEGURA_DP = 66
        const val PX_POR_DP = 4
        const val LIMITE_ALFA = 16
        const val ALTURA_MINIMA = 0.8
    }
}
