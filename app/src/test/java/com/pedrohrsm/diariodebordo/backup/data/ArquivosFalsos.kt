package com.pedrohrsm.diariodebordo.backup.data

import java.io.File
import java.io.InputStream

/** Fotos em memória e a cópia da exportação numa pasta temporária. */
class ArquivosFalsos(private val pasta: File) : ArquivosDoBackup {
    val fotos = mutableMapOf<String, ByteArray>()

    override fun abrirFoto(nome: String): InputStream? = fotos[nome]?.inputStream()
    override fun fotoExiste(nome: String) = nome in fotos
    override fun gravarFoto(nome: String, conteudo: InputStream) {
        fotos[nome] = conteudo.readBytes()
    }

    override fun apagarFoto(nome: String) = fotos.remove(nome) != null
    override fun ultimaExportacao() = File(pasta, "diario-de-bordo.zip")
}
