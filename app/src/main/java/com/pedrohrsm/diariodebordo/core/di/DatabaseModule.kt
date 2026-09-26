package com.pedrohrsm.diariodebordo.core.di

import android.content.Context
import androidx.room.Room
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
        Room.databaseBuilder(context, DiarioDatabase::class.java, DiarioDatabase.NOME).build()

    @Provides
    fun provideMotoDao(database: DiarioDatabase): MotoDao = database.motoDao()
}
