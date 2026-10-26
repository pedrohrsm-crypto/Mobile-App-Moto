package com.pedrohrsm.diariodebordo.backup.data

import android.content.Context
import android.net.Uri
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CanalDeDocumentosAndroid @Inject constructor(@ApplicationContext private val context: Context) :
    CanalDeDocumentos {

    // Tudo em Dispatchers.IO: o bloco (exportar ou importar) também lê e grava arquivos.
    // "wt": sobrescreve o conteúdo se a pessoa escolher um arquivo que já existe.
    override suspend fun <T> escrever(uri: String, bloco: suspend (OutputStream) -> T): T =
        withContext(Dispatchers.IO) {
            (context.contentResolver.openOutputStream(Uri.parse(uri), "wt") ?: throw FileNotFoundException(uri))
                .use { bloco(it) }
        }

    override suspend fun <T> ler(uri: String, bloco: suspend (InputStream) -> T): T = withContext(Dispatchers.IO) {
        (context.contentResolver.openInputStream(Uri.parse(uri)) ?: throw FileNotFoundException(uri)).use { bloco(it) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CanalDeDocumentosModule {
    @Binds
    abstract fun bindCanal(impl: CanalDeDocumentosAndroid): CanalDeDocumentos
}
