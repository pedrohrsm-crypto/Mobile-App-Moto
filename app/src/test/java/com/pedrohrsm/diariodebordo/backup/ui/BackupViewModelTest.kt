package com.pedrohrsm.diariodebordo.backup.ui

import androidx.room.Room
import com.pedrohrsm.diariodebordo.backup.data.ArquivosFalsos
import com.pedrohrsm.diariodebordo.backup.data.BackupRepositorio
import com.pedrohrsm.diariodebordo.backup.data.CanalDeDocumentos
import com.pedrohrsm.diariodebordo.backup.data.PacoteZip
import com.pedrohrsm.diariodebordo.core.database.DiarioDatabase
import com.pedrohrsm.diariodebordo.core.database.TransacaoRoom
import com.pedrohrsm.diariodebordo.core.premium.Premium
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import com.pedrohrsm.diariodebordo.rota.data.Rota
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupViewModelTest {

    private val relogio = Clock.fixed(Instant.parse("2026-12-01T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val banco = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), DiarioDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dao = banco.backupDao()
    private val repositorio = BackupRepositorio(
        dao,
        MotoRepositorio(banco.motoDao(), relogio),
        ArquivosFalsos(Files.createTempDirectory("backup").toFile()),
        TransacaoRoom(banco),
        AgendadorFalso(),
        Premium(habilitado = false),
        relogio,
    )

    /** Documentos do seletor do sistema, em memória: uri → bytes. */
    private val documentos = mutableMapOf<String, ByteArray>()
    private val canal = object : CanalDeDocumentos {
        override suspend fun <T> escrever(uri: String, bloco: suspend (OutputStream) -> T): T {
            val saida = ByteArrayOutputStream()
            return bloco(saida).also { documentos[uri] = saida.toByteArray() }
        }

        override suspend fun <T> ler(uri: String, bloco: suspend (InputStream) -> T): T =
            bloco(checkNotNull(documentos[uri]).inputStream())
    }

    @After
    fun fechar() = banco.close()

    private fun esperar(viewModel: BackupViewModel) {
        repeat(50) {
            ShadowLooper.idleMainLooper()
            if (!viewModel.estado.value.ocupado) return
            Thread.sleep(20)
        }
    }

    private val doArquivo = Moto(id = "m", apelido = "Do arquivo", marca = "Honda", modelo = "CG")
    private val doAparelho = Moto(id = "a", apelido = "Do aparelho", marca = "Yamaha", modelo = "Fazer")

    @Test
    fun importacaoQuePassaDoLimitePerguntaAMotoAntesDeGravar() {
        runBlocking { dao.gravarMotos(listOf(doArquivo)) }
        val vm = BackupViewModel(repositorio, canal, relogio)
        vm.exportar("content://backup.zip")
        esperar(vm)
        runBlocking { dao.gravarMotos(listOf(doArquivo.copy(excluidoEm = 1, alteradoEm = 1), doAparelho)) }

        vm.importar("content://backup.zip")
        esperar(vm)
        assertEquals(listOf("a", "m"), vm.estado.value.escolhendoMoto?.candidatas?.map { it.id })
        assertNull("nada gravado antes da escolha", vm.estado.value.resultado)
        assertEquals(listOf("a"), runBlocking { dao.motos() }.filter { it.excluidoEm == null }.map { it.id })

        vm.escolherMoto("a")
        esperar(vm)
        assertNull(vm.estado.value.escolhendoMoto)
        assertEquals(ResultadoBackup.Importado::class, vm.estado.value.resultado!!::class)
        assertEquals(listOf("a"), runBlocking { dao.motos() }.filter { it.excluidoEm == null }.map { it.id })
    }

    @Test
    fun exportarSemMarcarNadaDeixaAsRotasDeFora() {
        runBlocking {
            dao.gravarMotos(listOf(doArquivo))
            dao.gravarRotas(
                listOf(Rota(id = "rota", motoId = "m", iniciadaEm = 1, emGravacao = false, polilinha = "_p~iF~ps|U")),
            )
        }
        val vm = BackupViewModel(repositorio, canal, relogio)
        vm.pedirExportacao()
        assertFalse("o aviso abre com as rotas desmarcadas", vm.estado.value.comRotas)
        vm.exportar("content://backup.zip")
        esperar(vm)
        val copia = PacoteZip.lerDados(checkNotNull(documentos["content://backup.zip"]).inputStream())
        assertEquals(emptyList<Rota>(), copia.rotas)
    }

    @Test
    fun arquivoQueNaoEhBackupViraAvisoSemGravar() {
        documentos["content://foto.zip"] = "não é zip".toByteArray()
        val vm = BackupViewModel(repositorio, canal, relogio)
        vm.importar("content://foto.zip")
        esperar(vm)
        assertEquals(ResultadoBackup.ArquivoInvalido, vm.estado.value.resultado)
        assertFalse(vm.estado.value.ocupado)
    }
}
