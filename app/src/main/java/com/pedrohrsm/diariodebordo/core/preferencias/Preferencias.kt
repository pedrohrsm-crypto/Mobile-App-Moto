package com.pedrohrsm.diariodebordo.core.preferencias

import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import kotlinx.coroutines.flow.Flow

/** Preferências do aparelho. Não são dados do diário: não entram no arquivo exportado. */
interface Preferencias {
    /** Tema e modo oficina escolhidos; emite de novo a cada troca, e a tela muda sem recriar a Activity. */
    val ajuste: Flow<AjusteTema>

    /**
     * Leitura imediata do ajuste salvo, para o primeiro quadro já sair no tema certo (abertura a frio sem
     * piscar o tema de partida). Fora a abertura, use [ajuste].
     */
    fun ajusteSalvo(): AjusteTema

    fun salvar(ajuste: AjusteTema)
}
