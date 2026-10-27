package com.pedrohrsm.diariodebordo.plano.ui

import kotlinx.coroutines.CancellationException

/**
 * Como `runCatching`, mas relança o cancelamento: uma corrotina cancelada (a tela saiu) não vira "falha ao salvar".
 */
internal suspend inline fun <T> tentar(bloco: () -> T): Result<T> = try {
    Result.success(bloco())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
