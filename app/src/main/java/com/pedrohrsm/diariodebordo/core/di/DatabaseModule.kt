package com.pedrohrsm.diariodebordo.core.di

import android.content.Context
import androidx.room.Room
import com.pedrohrsm.diariodebordo.core.database.DiarioDatabase
import com.pedrohrsm.diariodebordo.core.database.Transacao
import com.pedrohrsm.diariodebordo.core.database.TransacaoRoom
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.RegistroDao
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.MotoDao
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlanoDao
import com.pedrohrsm.diariodebordo.rota.data.PontoDeRotaDao
import com.pedrohrsm.diariodebordo.rota.data.RotaDao
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
        Room.databaseBuilder(context, DiarioDatabase::class.java, DiarioDatabase.NOME).build()

    @Provides
    fun provideMotoDao(database: DiarioDatabase): MotoDao = database.motoDao()

    @Provides
    fun provideLeituraHodometroDao(database: DiarioDatabase): LeituraHodometroDao = database.leituraHodometroDao()

    @Provides
    fun provideRegistroDao(database: DiarioDatabase): RegistroDao = database.registroDao()

    @Provides
    fun provideFotoRegistroDao(database: DiarioDatabase): FotoRegistroDao = database.fotoRegistroDao()

    @Provides
    fun provideItemDoPlanoDao(database: DiarioDatabase): ItemDoPlanoDao = database.itemDoPlanoDao()

    @Provides
    fun provideRotaDao(database: DiarioDatabase): RotaDao = database.rotaDao()

    @Provides
    fun providePontoDeRotaDao(database: DiarioDatabase): PontoDeRotaDao = database.pontoDeRotaDao()

    @Provides
    fun provideTransacao(database: DiarioDatabase): Transacao = TransacaoRoom(database)
}
