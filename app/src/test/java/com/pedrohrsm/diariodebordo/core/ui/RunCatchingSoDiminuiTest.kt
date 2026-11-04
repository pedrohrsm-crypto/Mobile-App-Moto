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
 * muda a cada edição do arquivo).
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
        val achados = usosNoCodigo()
        assertEquals("runCatching novo: use tentar (core/ui/Tentar.kt)", emptyList<String>(), achados - USOS_DE_HOJE)
        assertEquals("uso retirado: tire-o também de USOS_DE_HOJE", emptyList<String>(), USOS_DE_HOJE - achados)
    }

    @Test
    fun aGuardaPegaAsVariantesDoRunCatching() {
        listOf(
            "val x = runCatching { carregar() }",
            "viewModelScope.launch { runCatching { acao() } }",
            "repositorio.runCatching { salvar() }",
            "kotlin.runCatching { salvar() }",
            "runCatching{ salvar() }",
        ).forEach { assertTrue(it, USO.containsMatchIn(it)) }
        listOf("tentar { salvar() }", "// sem runCatchingAqui", "fun meuRunCatching() {")
            .forEach { assertTrue(it, !USO.containsMatchIn(it)) }
    }

    private companion object {
        /** `runCatching {`, `.runCatching {` e `kotlin.runCatching {`; não pega nomes que só contêm a palavra. */
        val USO = Regex("""(^|[^A-Za-z0-9_])runCatching\s*\{""")

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
