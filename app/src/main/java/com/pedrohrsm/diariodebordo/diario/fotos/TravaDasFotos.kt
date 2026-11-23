package com.pedrohrsm.diariodebordo.diario.fotos

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Impede que a varredura de órfãs (#94) e a restauração do backup corram juntas. A restauração pode reativar uma
 * foto cujo arquivo antigo já está na pasta (ela não regrava o que existe): se a varredura lesse as fotos ativas
 * antes do commit, apagaria esse arquivo como órfão. Vale dentro do processo do app, onde o WorkManager também roda.
 */
@Singleton
class TravaDasFotos @Inject constructor() {
    private val mutex = Mutex()

    val ocupada: Boolean get() = mutex.isLocked

    /** A restauração espera a varredura terminar e segura a trava até o fim. */
    suspend fun <T> comTrava(bloco: suspend () -> T): T = mutex.withLock { bloco() }

    /** A varredura nunca espera: se a trava está com a restauração, devolve null e fica para depois. */
    suspend fun <T : Any> seLivre(bloco: suspend () -> T): T? {
        if (!mutex.tryLock()) return null
        try {
            return bloco()
        } finally {
            mutex.unlock()
        }
    }
}
