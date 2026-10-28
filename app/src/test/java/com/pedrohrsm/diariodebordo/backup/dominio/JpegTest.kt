package com.pedrohrsm.diariodebordo.backup.dominio

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class JpegTest {
    private fun segmento(marcador: Int, conteudo: ByteArray): ByteArray {
        val tamanho = conteudo.size + 2
        return byteArrayOf(0xFF.toByte(), marcador.toByte(), (tamanho shr 8).toByte(), tamanho.toByte()) + conteudo
    }

    private val soi = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
    private val jfif = segmento(0xE0, "JFIF\u0000".toByteArray())
    private val exifComGps = segmento(0xE1, "Exif\u0000\u0000GPS -23.55,-46.63".toByteArray())
    private val imagem = segmento(0xDA, byteArrayOf(1, 2, 3)) + byteArrayOf(0xFF.toByte(), 0xD9.toByte())

    @Test
    fun jpegImportadoSaiSemOExifComGps() {
        assertArrayEquals(soi + jfif + imagem, jpegSemMetadados(soi + exifComGps + jfif + imagem))
    }

    @Test
    fun jpegSemMetadadosPassaIgual() {
        assertArrayEquals(soi + jfif + imagem, jpegSemMetadados(soi + jfif + imagem))
    }

    @Test(expected = ArquivoInvalidoException::class)
    fun arquivoQueNaoEhJpegEhRecusado() {
        jpegSemMetadados("PK\u0003\u0004 não é foto".toByteArray())
    }

    @Test(expected = ArquivoInvalidoException::class)
    fun segmentoQuePassaDoFimEhRecusado() {
        jpegSemMetadados(soi + byteArrayOf(0xFF.toByte(), 0xE0.toByte(), 0x7F, 0x00, 1, 2))
    }
}
