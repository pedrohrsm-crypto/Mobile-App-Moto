package com.pedrohrsm.diariodebordo.diario.fotos

import com.pedrohrsm.diariodebordo.diario.data.FakeFotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistro
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.dominio.ArquivoNaPasta
import com.pedrohrsm.diariodebordo.diario.dominio.CARENCIA_DAS_ORFAS_MS
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Pasta de fotos em memória: nome → data de modificação. */
class PastaDeFotosFalsa : GuardaFotos {
    val arquivos = mutableMapOf<String, Long>()
    var falhaAoListar: Exception? = null

    /** Arquivo que o sistema recusa apagar (devolve false) e arquivo cujo apagar lança exceção. */
    val recusados = mutableSetOf<String>()
    val comExcecao = mutableSetOf<String>()

    override suspend fun importar(uri: String): String = error("A varredura nunca importa")

    override fun apagar(arquivo: String): Boolean {
        if (arquivo in comExcecao) throw SecurityException("Sem permissão")
        if (arquivo in recusados) return false
        arquivos.remove(arquivo)
        return true
    }

    override fun listar(): List<ArquivoNaPasta> {
        falhaAoListar?.let { throw it }
        return arquivos.map { (nome, quando) -> ArquivoNaPasta(nome, quando) }
    }
}

/**
 * A varredura de órfãs (#94, risco #79). O pior erro é apagar a foto de um registro ativo: leitura que falha aborta
 * sem apagar nada, e o arquivo recém-importado (o registro ainda não foi salvo) sobrevive pela carência.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VarreduraDeFotosOrfasTest {

    private val relogio = Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val agora = relogio.millis()
    private val velho = agora - CARENCIA_DAS_ORFAS_MS - 1
    private val pasta = PastaDeFotosFalsa()
    private val dao = FakeFotoRegistroDao()
    private val trava = TravaDasFotos()

    private fun varredura(fotos: FotoRegistroDao = dao) = VarreduraDeFotosOrfas(fotos, pasta, trava, relogio)

    private suspend fun ativa(arquivo: String) =
        dao.inserir(FotoRegistro(id = arquivo, registroId = "r", arquivo = arquivo, ordem = 0))

    @Test
    fun arquivoOrfaoForaDaCarenciaSaiDaPasta() = runTest {
        pasta.arquivos["orfa.jpg"] = velho

        val resultado = varredura().varrer()

        assertEquals(VarreduraDeFotosOrfas.Resultado.Feita(apagadas = 1, falharam = 0), resultado)
        assertFalse("orfa.jpg" in pasta.arquivos)
    }

    @Test
    fun arquivoDeFotoAtivaFicaNaPasta() = runTest {
        ativa("ativa.jpg")
        pasta.arquivos["ativa.jpg"] = velho
        pasta.arquivos["orfa.jpg"] = velho

        varredura().varrer()

        assertEquals(setOf("ativa.jpg"), pasta.arquivos.keys)
    }

    @Test
    fun arquivoRecemImportadoSemRegistroSobreviveAVarredura() = runTest {
        // Caso de corrida: o formulário já gravou o arquivo, mas o registro ainda não foi salvo.
        pasta.arquivos["importando.jpg"] = agora - 1_000

        val resultado = varredura().varrer()

        assertEquals(VarreduraDeFotosOrfas.Resultado.Feita(apagadas = 0, falharam = 0), resultado)
        assertTrue("importando.jpg" in pasta.arquivos)
    }

    @Test
    fun falhaAoApagarNaoDerrubaAVarreduraEEContada() = runTest {
        pasta.arquivos += mapOf("recusada.jpg" to velho, "excecao.jpg" to velho, "orfa.jpg" to velho)
        pasta.recusados += "recusada.jpg"
        pasta.comExcecao += "excecao.jpg"

        val resultado = varredura().varrer()

        assertEquals(VarreduraDeFotosOrfas.Resultado.Feita(apagadas = 1, falharam = 2), resultado)
        // As duas que falharam ficam para a próxima varredura.
        assertEquals(setOf("recusada.jpg", "excecao.jpg"), pasta.arquivos.keys)
    }

    @Test
    fun leituraDasFotosAtivasQueFalhaAbortaSemApagarNada() = runTest {
        pasta.arquivos += mapOf("ativa.jpg" to velho, "outra.jpg" to velho)
        val quebrado = object : FotoRegistroDao by dao {
            override suspend fun arquivosAtivos(): List<String> = throw IllegalStateException("banco indisponível")
        }

        try {
            varredura(quebrado).varrer()
            fail("A varredura deveria abortar")
        } catch (_: IllegalStateException) {
        }

        assertEquals(setOf("ativa.jpg", "outra.jpg"), pasta.arquivos.keys)
    }

    @Test
    fun pastaIlegivelAbortaSemApagarNada() = runTest {
        pasta.arquivos["orfa.jpg"] = velho
        pasta.falhaAoListar = IOException("Pasta de fotos ilegível")

        try {
            varredura().varrer()
            fail("A varredura deveria abortar")
        } catch (_: IOException) {
        }

        assertTrue("orfa.jpg" in pasta.arquivos)
    }

    @Test
    fun varreduraComARestauracaoEmAndamentoFicaParaDepois() = runTest {
        pasta.arquivos["orfa.jpg"] = velho
        val fimDaRestauracao = CompletableDeferred<Unit>()
        val restauracao = launch { trava.comTrava { fimDaRestauracao.await() } }
        runCurrent()

        val resultado = varredura().varrer()

        assertEquals(VarreduraDeFotosOrfas.Resultado.Adiada, resultado)
        assertTrue("nada lido nem apagado", "orfa.jpg" in pasta.arquivos)
        fimDaRestauracao.complete(Unit)
        restauracao.join()
        assertEquals(VarreduraDeFotosOrfas.Resultado.Feita(apagadas = 1, falharam = 0), varredura().varrer())
    }
}
