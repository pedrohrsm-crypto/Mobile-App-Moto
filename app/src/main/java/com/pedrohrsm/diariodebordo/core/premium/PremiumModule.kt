package com.pedrohrsm.diariodebordo.core.premium

import com.pedrohrsm.diariodebordo.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object PremiumModule {
    @Provides
    fun providePremium(): Premium = Premium(BuildConfig.PREMIUM_HABILITADO)
}
