package com.pedrohrsm.diariodebordo.backup.dominio

import java.io.ByteArrayOutputStream

/**
 * Foto que chega por um arquivo importado: precisa ser JPEG, e sai sem os blocos de metadados (APP1, onde ficam o
 * EXIF e o GPS, e APP13), como as fotos que o próprio app grava (risco #23). Percorre os segmentos até o início
 * da imagem (SOS); dali em diante os bytes seguem iguais.
 */
fun jpegSemMetadados(bytes: ByteArray): ByteArray {
    fun u(i: Int) = bytes[i].toInt() and 0xFF
    if (bytes.size < 4 || u(0) != 0xFF || u(1) != 0xD8) throw ArquivoInvalidoException("Foto que não é JPEG")
    val saida = ByteArrayOutputStream(bytes.size)
    saida.write(bytes, 0, 2)
    var i = 2
    while (true) {
        if (i + 4 > bytes.size || u(i) != 0xFF) throw ArquivoInvalidoException("JPEG corrompido")
        val marcador = u(i + 1)
        if (marcador == SOS) {
            saida.write(bytes, i, bytes.size - i)
            return saida.toByteArray()
        }
        val tamanho = (u(i + 2) shl 8) or u(i + 3)
        val fim = i + 2 + tamanho
        if (tamanho < 2 || fim > bytes.size) throw ArquivoInvalidoException("JPEG corrompido")
        if (marcador != APP1 && marcador != APP13) saida.write(bytes, i, fim - i)
        i = fim
    }
}

private const val SOS = 0xDA
private const val APP1 = 0xE1
private const val APP13 = 0xED
