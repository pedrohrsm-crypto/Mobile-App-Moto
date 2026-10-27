package com.pedrohrsm.diariodebordo.backup.data

import java.io.InputStream
import java.io.OutputStream

/**
 * Abre o documento que a pessoa escolheu no seletor do sistema (uri em texto). Não precisa de
 * permissão de armazenamento. Interface para o ViewModel ser testado na JVM.
 */
interface CanalDeDocumentos {
    suspend fun <T> escrever(uri: String, bloco: suspend (OutputStream) -> T): T

    suspend fun <T> ler(uri: String, bloco: suspend (InputStream) -> T): T
}
