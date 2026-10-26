package com.pedrohrsm.diariodebordo.rota.servico

import com.pedrohrsm.diariodebordo.rota.ui.StatusCaptura
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class StatusCapturaServico @Inject constructor() : StatusCaptura {
    override val ativo: Flow<Boolean> = GravacaoRotaService.gravando
    override val falha: Flow<String?> = GravacaoRotaService.falha
    override fun limparFalha() = GravacaoRotaService.limparFalha()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RotaModule {
    @Binds
    abstract fun bindStatus(impl: StatusCapturaServico): StatusCaptura
}
