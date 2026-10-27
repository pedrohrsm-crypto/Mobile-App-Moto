package com.pedrohrsm.diariodebordo.backup.data

import java.io.File
import java.io.InputStream

/** Arquivos do app que o backup lê e escreve. Interface para testar na JVM com uma pasta temporária. */
interface ArquivosDoBackup {
    /** Conteúdo da foto do diário, ou null se o arquivo sumiu. */
    fun abrirFoto(nome: String): InputStream?

    fun fotoExiste(nome: String): Boolean

    fun gravarFoto(nome: String, conteudo: ByteArray)

    fun apagarFoto(nome: String): Boolean

    /** Bytes livres onde as fotos ficam: a importação nunca grava tanto que o aparelho fique sem espaço. */
    fun espacoLivre(): Long

    /**
     * Cópia do último arquivo exportado, em files/exportacao/. É o único arquivo que o backup
     * automático do Android leva (res/xml/backup_rules.xml), nunca o banco aberto.
     */
    fun ultimaExportacao(): File
}
