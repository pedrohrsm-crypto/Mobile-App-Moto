package com.pedrohrsm.diariodebordo.core.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guarda da regra da segunda ocorrência (CLAUDE.md; #43 no plano, #72 na exclusão de moto): `runCatching` pega
 * `Throwable` e engole o `CancellationException`, e uma corrotina cancelada vira "falha ao salvar". O caminho é
 * [tentar]. Catraca: os usos que existiam em 4-Nov-2026 estão em [USOS_DE_HOJE] e a lista só diminui. Um uso novo
 * reprova, inclusive num arquivo que ainda não existe; um uso retirado sem sair da lista também reprova, para a
 * lista encolher junto. Cada uso é identificado pelo arquivo e pela linha de código (sem o número da linha, que
 * muda a cada edição do arquivo), e a comparação conta as repetições: dois usos com o mesmo texto no mesmo arquivo
 * valem dois. A busca é por linha: um `runCatching` com a chave `{` só na linha seguinte escapa (o ktlint quase
 * sempre deixa a chave na mesma linha).
 */
class RunCatchingSoDiminuiTest {

    private val raiz = File("src/main/java/com/pedrohrsm/diariodebordo")

    private fun usosNoCodigo(): List<String> = raiz.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .flatMap { arquivo ->
            arquivo.readLines().filter { USO.containsMatchIn(it) }
                .map { "${arquivo.relativeTo(raiz).invariantSeparatorsPath}: ${it.trim()}" }
        }
        .sorted()
        .toList()

    @Test
    fun runCatchingNovoReprovaEAListaSoDiminui() {
        assertTrue("rode da pasta app/: ${raiz.absolutePath}", raiz.isDirectory)
        val achados = usosNoCodigo().groupingBy { it }.eachCount()
        val hoje = USOS_DE_HOJE.groupingBy { it }.eachCount()
        assertEquals("runCatching novo: use tentar (core/ui/Tentar.kt)", emptyList<String>(), aMais(achados, hoje))
        assertEquals("uso retirado: tire-o também de USOS_DE_HOJE", emptyList<String>(), aMais(hoje, achados))
    }

    /** O que [de] tem a mais que [que], contando repetições: "arquivo: linha (antes → agora)". */
    private fun aMais(de: Map<String, Int>, que: Map<String, Int>): List<String> =
        de.filter { (uso, vezes) -> vezes > (que[uso] ?: 0) }
            .map { (uso, vezes) -> "$uso (${que[uso] ?: 0} → $vezes)" }
            .sorted()

    @Test
    fun aGuardaPegaAsVariantesDoRunCatching() {
        listOf(
            "val x = runCatching { carregar() }",
            "viewModelScope.launch { runCatching { acao() } }",
            "repositorio.runCatching { salvar() }",
            "kotlin.runCatching { salvar() }",
            "runCatching{ salvar() }",
            "val r = runCatching<Unit> { salvar() }",
        ).forEach { assertTrue(it, USO.containsMatchIn(it)) }
        listOf("tentar { salvar() }", "// sem runCatchingAqui", "fun meuRunCatching() {")
            .forEach { assertTrue(it, !USO.containsMatchIn(it)) }
    }

    private companion object {
        /** `runCatching {`, `.runCatching {`, `kotlin.runCatching {` e `runCatching<T> {`; não pega nomes que só contêm a palavra. */
        val USO = Regex("""(^|[^A-Za-z0-9_])runCatching\s*(<[^>]*>)?\s*\{""")

        /** Medidos em 4-Nov-2026 (head 0a8b4c1, `grep -rnE` sobre app/src/main/java): 13 usos. */
        val USOS_DE_HOJE = listOf(
            "diario/ui/RegistroFormViewModel.kt: runCatching { carregar(tipoInicial) }",
            "diario/ui/RegistroFormViewModel.kt: runCatching { guardaFotos.importar(uri) }",
            "diario/ui/RegistroFormViewModel.kt: runCatching { localizacao.atual() }",
            "diario/ui/RegistroFormViewModel.kt: runCatching { repositorio.excluir(id) }",
            "diario/ui/RegistroFormViewModel.kt: runCatching { repositorio.salvar(registro, e.fotos) }",
            "garagem/ui/MotoDetalheViewModel.kt: runCatching {",
            "garagem/ui/MotoDetalheViewModel.kt: runCatching { hodometro.apagar(atual.leitura.id) }",
            "garagem/ui/MotoFormViewModel.kt: runCatching {",
            "garagem/ui/MotoFormViewModel.kt: val moto = runCatching { motos.observar(motoId).first() }",
            "numeros/Formatador.kt: fun fuso(id: String): ZoneId = runCatching { ZoneId.of(id) }" +
                ".getOrDefault(ZoneId.systemDefault())",
            "rota/servico/GravacaoRotaService.kt: runCatching { rotas.emGravacao()?.let { rotas.finalizar(it.id, " +
                "getString(R.string.rota_titulo_role)) } }",
            "rota/ui/RotaDetalheViewModel.kt: runCatching { rotas.excluir(rotaId) }",
            "rota/ui/RotasViewModel.kt: viewModelScope.launch { runCatching { acao() }" +
                ".onFailure { _falha.value = FalhaRota.AoSalvar(it.message) } }",
        ).sorted()
    }
}
