package com.pedrohrsm.diariodebordo.garagem.dominio

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guarda da regra única (04, categoria 8; revisão de 26-Out, HodometroRepositorio.kt:48): comparar km com
 * `<`, `<=`, `>` ou `>=` é regra de negócio e só mora nas funções de domínio puras (pacotes `dominio/`).
 * Repositório, ViewModel ou tela que compare km reprova, inclusive um arquivo que ainda não existe;
 * a saída é chamar a regra do domínio (no hodômetro, `validarLeitura`).
 */
class RegraDeKmSoNoDominioTest {

    private val raiz = File("src/main/java")
    private val comparacaoDeKm = Regex(
        """(?i)\b\w*km\w*\s+(<=|>=|<|>)\s+|\s(<=|>=|<|>)\s+\w*km\w*\b""",
    )

    @Test
    fun comparacaoDeKmSoMoraNoDominio() {
        assertTrue("rode da pasta app/: ${raiz.absolutePath}", raiz.isDirectory)
        val fora = raiz.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && "/dominio/" !in it.invariantSeparatorsPath }
            .flatMap { arquivo ->
                arquivo.readLines().mapIndexedNotNull { i, linha ->
                    "${arquivo.relativeTo(raiz).invariantSeparatorsPath}:${i + 1}: ${linha.trim()}"
                        .takeIf { comparacaoDeKm.containsMatchIn(linha) }
                }
            }
            .toList()
        assertEquals("compare km pela regra do domínio, não à mão", emptyList<String>(), fora)
    }

    @Test
    fun aGuardaPegaAsVariantesDaMesmaRegra() {
        listOf("if (atual != null && km <= atual) return false", "kmNovo < kmAnterior", "if (leitura.km >= limite)")
            .forEach { assertTrue(it, comparacaoDeKm.containsMatchIn(it)) }
        listOf("List<KmAtual>", "{ km -> km }", "ano < 1900")
            .forEach { assertTrue(it, !comparacaoDeKm.containsMatchIn(it)) }
    }
}
