package com.pedrohrsm.diariodebordo.backup.data

import android.content.Context
import com.pedrohrsm.diariodebordo.diario.fotos.ArmazenamentoFotos
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.InputStream
import javax.inject.Inject

class ArquivosDoBackupAndroid @Inject constructor(@ApplicationContext private val context: Context) : ArquivosDoBackup {
    private val fotos: File get() = File(context.filesDir, ArmazenamentoFotos.PASTA).apply { mkdirs() }

    override fun abrirFoto(nome: String): InputStream? = File(fotos, nome).takeIf { it.isFile }?.inputStream()

    override fun fotoExiste(nome: String): Boolean = File(fotos, nome).isFile

    override fun gravarFoto(nome: String, conteudo: ByteArray) {
        // Grava num temporário e renomeia: foto pela metade nunca fica com o nome final.
        val temporario = File(fotos, "$nome.parcial")
        temporario.writeBytes(conteudo)
        check(temporario.renameTo(File(fotos, nome))) { "Não foi possível gravar a foto $nome" }
    }

    override fun apagarFoto(nome: String): Boolean = File(fotos, nome).delete()

    override fun espacoLivre(): Long = fotos.usableSpace

    override fun ultimaExportacao(): File =
        File(File(context.filesDir, PASTA_EXPORTACAO).apply { mkdirs() }, "diario-de-bordo.zip")

    private companion object {
        /** Mesma pasta das regras de backup automático em res/xml. */
        const val PASTA_EXPORTACAO = "exportacao"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {
    @Binds
    abstract fun bindArquivos(impl: ArquivosDoBackupAndroid): ArquivosDoBackup
}
