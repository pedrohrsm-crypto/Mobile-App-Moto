package com.pedrohrsm.diariodebordo.core.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.pedrohrsm.diariodebordo.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Guarda do ícone do launcher (#84). O símbolo precisa caber nos 66 dp centrais dos 108 dp do ícone adaptativo: fora
 * disso a máscara do fabricante (círculo, quadrado arredondado, gota) corta o guidão. O teste desenha o vetor de
 * verdade (4 px por dp) e mede a caixa dos pixels pintados, em vez de conferir os números do XML.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class IconeLauncherTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun simboloDoLauncherCabeNaZonaSeguraDe66DpDoIconeAdaptativo() {
        val caixa = caixaPintada()
        val margemMinima = (TAMANHO_DP - ZONA_SEGURA_DP) / 2 * PX_POR_DP
        assertTrue("esquerda ${caixa.esquerda} px dentro da zona segura", caixa.esquerda >= margemMinima)
        assertTrue("topo ${caixa.topo} px dentro da zona segura", caixa.topo >= margemMinima)
        val limite = TAMANHO_DP * PX_POR_DP - margemMinima
        assertTrue("direita ${caixa.direita} px dentro da zona segura", caixa.direita <= limite)
        assertTrue("base ${caixa.base} px dentro da zona segura", caixa.base <= limite)
    }

    @Test
    fun simboloDoLauncherFicaCentralizadoEOcupaAZonaSegura() {
        val caixa = caixaPintada()
        val centro = TAMANHO_DP * PX_POR_DP / 2
        assertEquals("centro horizontal", centro.toDouble(), (caixa.esquerda + caixa.direita) / 2.0, 2.0)
        assertEquals("centro vertical", centro.toDouble(), (caixa.topo + caixa.base) / 2.0, 2.0)
        // Símbolo pequeno demais some em 48 dp: pede ao menos 90% da altura da zona segura.
        assertTrue("altura ${caixa.base - caixa.topo} px", caixa.base - caixa.topo >= ZONA_SEGURA_DP * PX_POR_DP * 0.9)
    }

    @Test
    fun fundoDoIconeAdaptativoEOGrafiteDaIdentidadeVisual() {
        assertEquals(0xFF1A1C1E.toInt(), contexto.getColor(R.color.ic_launcher_background))
    }

    private data class Caixa(val esquerda: Int, val topo: Int, val direita: Int, val base: Int)

    private fun caixaPintada(): Caixa {
        val lado = TAMANHO_DP * PX_POR_DP
        val bitmap = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        val vetor = checkNotNull(ContextCompat.getDrawable(contexto, R.drawable.ic_launcher_foreground))
        vetor.setBounds(0, 0, lado, lado)
        vetor.draw(Canvas(bitmap))
        var esquerda = lado
        var topo = lado
        var direita = -1
        var base = -1
        for (y in 0 until lado) {
            for (x in 0 until lado) {
                if (bitmap.getPixel(x, y) ushr 24 > LIMITE_ALFA) {
                    if (x < esquerda) esquerda = x
                    if (y < topo) topo = y
                    if (x > direita) direita = x
                    if (y > base) base = y
                }
            }
        }
        assertTrue("o vetor não pintou nada", direita >= 0)
        return Caixa(esquerda, topo, direita + 1, base + 1)
    }

    private companion object {
        const val TAMANHO_DP = 108
        const val ZONA_SEGURA_DP = 66
        const val PX_POR_DP = 4
        const val LIMITE_ALFA = 16
    }
}
