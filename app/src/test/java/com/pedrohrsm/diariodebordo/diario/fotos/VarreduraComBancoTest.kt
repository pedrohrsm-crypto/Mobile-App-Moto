package com.pedrohrsm.diariodebordo.diario.fotos

import android.app.Application
import androidx.room.Room
import com.pedrohrsm.diariodebordo.core.database.DiarioDatabase
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistro
import com.pedrohrsm.diariodebordo.diario.dominio.CARENCIA_DAS_ORFAS_MS
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * A varredura com o caminho do app (#94): a consulta das fotos ativas no Room de verdade e a pasta de verdade do
 * [ArmazenamentoFotos], com a data de modificação lida do disco.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VarreduraComBancoTest {

    private val contexto: Application = RuntimeEnvironment.getApplication()
    private val banco = Room.inMemoryDatabaseBuilder(contexto, DiarioDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val relogio = Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val armazenamento = ArmazenamentoFotos(contexto)
    private val pasta = File(contexto.filesDir, ArmazenamentoFotos.PASTA)
    private val varredura = VarreduraDeFotosOrfas(banco.fotoRegistroDao(), armazenamento, TravaDasFotos(), relogio)

    @After
    fun fechar() {
        banco.close()
        File(contexto.filesDir, ArmazenamentoFotos.PASTA).deleteRecursively()
    }

    private fun arquivo(nome: String, modificadoEm: Long = relogio.millis() - CARENCIA_DAS_ORFAS_MS - 1): File =
        armazenamento.arquivo(nome).apply {
            writeBytes(byteArrayOf(1, 2, 3))
            check(setLastModified(modificadoEm))
        }

    @Test
    fun fotoExcluidaComArquivoPresenteSaiEAAtivaFica() = runTest {
        val fotos = banco.fotoRegistroDao()
        fotos.inserir(FotoRegistro(id = "a", registroId = "r", arquivo = "ativa.jpg", ordem = 0))
        fotos.inserir(FotoRegistro(id = "x", registroId = "r", arquivo = "excluida.jpg", ordem = 1, excluidoEm = 5))
        arquivo("ativa.jpg")
        arquivo("excluida.jpg")

        val resultado = varredura.varrer()

        assertEquals(VarreduraDeFotosOrfas.Resultado.Feita(apagadas = 1, falharam = 0), resultado)
        assertEquals(listOf("ativa.jpg"), pasta.list()?.toList())
    }

    @Test
    fun listarDevolveOsArquivosDaPastaComADataDeModificacao() {
        val quando = relogio.millis() - 3_600_000
        arquivo("uma.jpg", quando)

        val lista = armazenamento.listar()

        assertEquals("uma.jpg", lista.single().nome)
        // O sistema de arquivos pode arredondar para o segundo.
        assertTrue(kotlin.math.abs(lista.single().modificadoEm - quando) < 1_000)
    }

    @Test
    fun pastaIlegivelLancaEmVezDeVirarListaVazia() {
        pasta.deleteRecursively()
        // No lugar da pasta, um arquivo comum: listFiles devolve null.
        pasta.writeBytes(byteArrayOf(0))

        try {
            armazenamento.listar()
            fail("Pasta ilegível não pode virar lista vazia")
        } catch (_: IOException) {
        } finally {
            pasta.delete()
        }
    }
}
