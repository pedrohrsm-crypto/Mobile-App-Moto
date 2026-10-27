package com.pedrohrsm.diariodebordo.core.di

import android.content.Context
import androidx.room.Room
import com.pedrohrsm.diariodebordo.BuildConfig
import com.pedrohrsm.diariodebordo.core.database.DiarioDatabase
import com.pedrohrsm.diariodebordo.core.database.Transacao
import com.pedrohrsm.diariodebordo.core.database.TransacaoRoom
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.RegistroDao
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometroDao
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
     * Decisão #12: só o build de debug recria o banco quando a versão gravada no aparelho difere da do app
     * e falta Migration; no release isso é erro, e o diário nunca é apagado. Com a versão 1 aberta, o schema
     * muda sem mudar a versão: aí o Room recusa abrir até no debug (identity hash), e o aparelho de teste
     * precisa ter os dados do app limpos. `DatabaseModuleTest` prova os três casos.
     */
    fun criarBanco(context: Context, nome: String, debug: Boolean): DiarioDatabase {
        val construtor = Room.databaseBuilder(context, DiarioDatabase::class.java, nome)
        if (debug) construtor.fallbackToDestructiveMigration(dropAllTables = true)
        return construtor.build()
    }

    @Provides
    fun provideMotoDao(database: DiarioDatabase): MotoDao = database.motoDao()

    @Provides
    fun provideLeituraHodometroDao(database: DiarioDatabase): LeituraHodometroDao = database.leituraHodometroDao()

    @Provides
    fun provideRegistroDao(database: DiarioDatabase): RegistroDao = database.registroDao()

    @Provides
    fun provideFotoRegistroDao(database: DiarioDatabase): FotoRegistroDao = database.fotoRegistroDao()

    @Provides
    fun provideTransacao(database: DiarioDatabase): Transacao = TransacaoRoom(database)
}
