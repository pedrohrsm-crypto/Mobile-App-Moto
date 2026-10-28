package com.pedrohrsm.diariodebordo.backup.data

import java.io.File
import java.io.InputStream

/** Fotos em memória e a cópia da exportação numa pasta temporária. [espaco] é o espaço livre do aparelho. */
class ArquivosFalsos(private val pasta: File, var espaco: Long = Long.MAX_VALUE / 2) : ArquivosDoBackup {
    val fotos = mutableMapOf<String, ByteArray>()

    override fun abrirFoto(nome: String): InputStream? = fotos[nome]?.inputStream()
    override fun fotoExiste(nome: String) = nome in fotos
    override fun gravarFoto(nome: String, conteudo: ByteArray) {
        fotos[nome] = conteudo
    }

    override fun apagarFoto(nome: String) = fotos.remove(nome) != null
    override fun espacoLivre() = espaco
    override fun ultimaExportacao() = File(pasta, "diario-de-bordo.zip")
}
