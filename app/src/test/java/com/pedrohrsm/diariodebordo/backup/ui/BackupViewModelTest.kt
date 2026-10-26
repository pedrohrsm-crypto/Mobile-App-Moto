package com.pedrohrsm.diariodebordo.backup.ui

import com.pedrohrsm.diariodebordo.backup.data.ArquivosFalsos
import com.pedrohrsm.diariodebordo.backup.data.BackupRepositorio
import com.pedrohrsm.diariodebordo.backup.data.CanalDeDocumentos
import com.pedrohrsm.diariodebordo.backup.data.FakeBackupDao
import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Documentos do seletor do sistema guardados em memória, pelo uri. */
class CanalFalso : CanalDeDocumentos {
    val documentos = mutableMapOf<String, ByteArray>()

    override suspend fun <T> escrever(uri: String, bloco: suspend (OutputStream) -> T): T {
        val saida = ByteArrayOutputStream()
        return bloco(saida).also { documentos[uri] = saida.toByteArray() }
    }

    override suspend fun <T> ler(uri: String, bloco: suspend (InputStream) -> T): T =
        bloco(checkNotNull(documentos[uri]).inputStream())
}

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private val relogio = Clock.fixed(Instant.parse("2026-12-01T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val dao = FakeBackupDao().apply {
        motos = listOf(Moto(id = "m", apelido = "Minha moto", marca = "Honda", modelo = "CG 160"))
    }
    private val canal = CanalFalso()
    private val repositorio = BackupRepositorio(
        dao,
        ArquivosFalsos(Files.createTempDirectory("backup").toFile()),
        TransacaoFalsa(),
        AgendadorFalso(),
        relogio,
    )
    private val despachante = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(despachante)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun nomeSugeridoTemAData() {
        assertEquals("diario-de-bordo-2026-12-01.zip", BackupViewModel(repositorio, canal, relogio).nomeSugerido())
    }

    @Test
    fun exportarPassaPeloAvisoEGravaNoDestinoEscolhido() = runTest(despachante) {
        val vm = BackupViewModel(repositorio, canal, relogio)
        vm.pedirExportacao()
        assertTrue(vm.estado.value.confirmandoExportacao)
        vm.alternarRotas(false)
        vm.exportar("content://destino")
        assertFalse(vm.estado.value.confirmandoExportacao)
        advanceUntilIdle()
        val resultado = vm.estado.value.resultado as ResultadoBackup.Exportado
        assertEquals(1, resultado.resumo.motos)
        assertTrue(canal.documentos.getValue("content://destino").isNotEmpty())
        assertFalse(vm.estado.value.ocupado)
    }

    @Test
    fun seletorFechadoNaoFazNada() = runTest(despachante) {
        val vm = BackupViewModel(repositorio, canal, relogio)
        vm.pedirExportacao()
        vm.exportar(null)
        vm.importar(null)
        advanceUntilIdle()
        assertEquals(BackupEstado(), vm.estado.value)
        assertTrue(canal.documentos.isEmpty())
    }

    @Test
    fun arquivoQualquerViraAvisoDeArquivoInvalido() = runTest(despachante) {
        canal.documentos["content://foto"] = byteArrayOf(1, 2, 3)
        val vm = BackupViewModel(repositorio, canal, relogio)
        vm.importar("content://foto")
        advanceUntilIdle()
        assertEquals(ResultadoBackup.ArquivoInvalido, vm.estado.value.resultado)
    }
}
