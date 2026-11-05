package com.pedrohrsm.diariodebordo.exemplo

import androidx.compose.runtime.Composable

/**
 * No release não existe ação de dados de exemplo (#77): esta versão não desenha nada e, por ser `inline`, nem
 * deixa classe nem chamada no AAB (o do debug está em src/debug, com o botão de verdade).
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun SecaoDadosDeExemplo() = Unit
