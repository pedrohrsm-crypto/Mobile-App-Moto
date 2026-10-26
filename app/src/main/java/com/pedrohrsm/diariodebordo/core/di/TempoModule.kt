package com.pedrohrsm.diariodebordo.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/** Relógio injetável: os repositórios leem a hora e o fuso daqui, e os testes usam Clock.fixed. */
@Module
@InstallIn(SingletonComponent::class)
object TempoModule {

    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
