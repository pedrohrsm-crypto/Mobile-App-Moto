package com.pedrohrsm.diariodebordo.core.di

import android.content.Context
import androidx.room.Room
import com.pedrohrsm.diariodebordo.BuildConfig
import com.pedrohrsm.diariodebordo.core.database.DiarioDatabase
import com.pedrohrsm.diariodebordo.garagem.data.MotoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DiarioDatabase =
        criarBanco(context, DiarioDatabase.NOME, debug = BuildConfig.DEBUG)

    /**
     * Decisão #12: a versão 1 fica aberta até a primeira publicação, e só o build de debug recria o banco
     * quando o schema muda sem Migration. No release, faltar Migration é erro: o diário nunca é apagado.
     */
    fun criarBanco(context: Context, nome: String, debug: Boolean): DiarioDatabase {
        val construtor = Room.databaseBuilder(context, DiarioDatabase::class.java, nome)
        if (debug) construtor.fallbackToDestructiveMigration(dropAllTables = true)
        return construtor.build()
    }

    @Provides
    fun provideMotoDao(database: DiarioDatabase): MotoDao = database.motoDao()
}
