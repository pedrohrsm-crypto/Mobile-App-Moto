package com.pedrohrsm.diariodebordo.backup.data

import androidx.room.Room
import com.pedrohrsm.diariodebordo.backup.dominio.ArquivoInvalidoException
import com.pedrohrsm.diariodebordo.backup.dominio.EscolhaDeMotoNecessariaException
import com.pedrohrsm.diariodebordo.backup.dominio.EspacoInsuficienteException
import com.pedrohrsm.diariodebordo.core.database.DiarioDatabase
import com.pedrohrsm.diariodebordo.core.database.TransacaoRoom
import com.pedrohrsm.diariodebordo.core.premium.Premium
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistro
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.CategoriaMoto
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.OrigemItem
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import com.pedrohrsm.diariodebordo.rota.data.PontoDeRota
import com.pedrohrsm.diariodebordo.rota.data.Rota
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Exportar num aparelho e importar noutro, com o banco de verdade (Room no SQLite do Robolectric): as consultas do
 * [BackupDao], a transação e as 7 tabelas da versão 1 passam pelo mesmo caminho do app.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupRepositorioTest {

    private val relogio = Clock.fixed(Instant.parse("2026-12-01T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val abertos = mutableListOf<DiarioDatabase>()

    @After
    fun fechar() = abertos.forEach { it.close() }

    private inner class Aparelho(
        premium: Premium = Premium(habilitado = false),
        dao: (BackupDao) -> BackupDao = {
            it
        },
    ) {
        val banco: DiarioDatabase = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            DiarioDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
            .also { abertos += it }
        val backup = banco.backupDao()
        val arquivos = ArquivosFalsos(Files.createTempDirectory("backup").toFile())
        val agendador = AgendadorFalso()
        val repositorio = BackupRepositorio(
            dao(backup),
            MotoRepositorio(banco.motoDao(), relogio),
            arquivos,
            TransacaoRoom(banco),
            agendador,
            premium,
            relogio,
        )
    }

    private val moto = Moto(
        id = "m",
        apelido = "Minha moto",
        marca = "Honda",
        modelo = "CG 160",
        ano = 2022,
        categoria = CategoriaMoto.STREET,
        criadoEm = 1,
        alteradoEm = 10,
        fusoHorario = "America/Sao_Paulo",
    )
    private val leitura =
        LeituraHodometro(id = "l", motoId = "m", km = 15_000, lidaEm = 3, criadoEm = 3, alteradoEm = 10)
    private val rota = Rota(
        id = "rota",
        motoId = "m",
        iniciadaEm = 1,
        terminadaEm = 2,
        distanciaM = 12_345,
        duracaoS = 600,
        polilinha = "_p~iF~ps|U",
        emGravacao = false,
        alteradoEm = 10,
    )
    private val role = Registro(
        id = "role",
        motoId = "m",
        tipo = TipoRegistro.ROLE,
        titulo = "Rolê",
        dataEm = 1,
        rotaId = "rota",
        latitude = -23.55,
        longitude = -46.63,
        alteradoEm = 10,
    )
    private val oleo = Registro(
        id = "oleo",
        motoId = "m",
        tipo = TipoRegistro.MANUTENCAO,
        titulo = "Óleo",
        dataEm = 2,
        hodometroKm = 15_000,
        valorCentavos = 8_990,
        alteradoEm = 10,
    )
    private val foto = FotoRegistro(id = "f", registroId = "oleo", arquivo = "oleo.jpg", ordem = 0, alteradoEm = 10)
    private val item = ItemDoPlano(
        id = "i",
        motoId = "m",
        chave = "troca_de_oleo",
        nome = "Troca de óleo",
        intervaloKm = 5_000,
        origem = OrigemItem.PLANO_BASE,
        alteradoEm = 10,
    )

    /** Aparelho com tudo da versão 1, inclusive o que não pode sair: excluídos e a rota em gravação com os pontos. */
    private suspend fun Aparelho.cheio(): Aparelho = apply {
        backup.gravarMotos(
            listOf(moto, Moto(id = "velha", apelido = "Vendida", marca = "Honda", modelo = "CG", excluidoEm = 5)),
        )
        backup.gravarLeituras(listOf(leitura))
        backup.gravarRotas(listOf(rota, rota.copy(id = "apagada", polilinha = "", excluidoEm = 7)))
        banco.rotaDao().inserir(Rota(id = "gravando", motoId = "m", iniciadaEm = 50, emGravacao = true))
        banco.pontoDeRotaDao().inserir(
            PontoDeRota(rotaId = "gravando", latitude = -23.5, longitude = -46.6, instante = 51),
        )
        backup.gravarRegistros(
            listOf(
                role,
                oleo,
                Registro(
                    id = "apagado",
                    motoId = "m",
                    tipo = TipoRegistro.NOTA,
                    titulo = "Segredo",
                    dataEm = 3,
                    excluidoEm = 4,
                ),
            ),
        )
        backup.gravarFotos(listOf(foto))
        backup.gravarItensDoPlano(listOf(item))
        arquivos.fotos["oleo.jpg"] = JPEG
    }

    private suspend fun Aparelho.exportar(comRotas: Boolean = true): Pair<ByteArray, ResumoExportacao> {
        val saida = ByteArrayOutputStream()
        val resumo = repositorio.exportar(saida, comRotas)
        return saida.toByteArray() to resumo
    }

    private fun Aparelho.contar(tabela: String): Int =
        banco.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $tabela").use {
            it.moveToFirst()
            it.getInt(0)
        }

    @Test
    fun exportarEImportarNoutroAparelhoRecuperaAsSeteTabelas() = runTest {
        val (arquivo, resumo) = Aparelho().cheio().exportar()
        assertEquals(ResumoExportacao(motos = 1, registros = 2, fotos = 1, rotas = 1, fotosFaltando = 0), resumo)

        val novo = Aparelho()
        val importado = novo.repositorio.importar(arquivo.inputStream())

        assertEquals(ResumoImportacao(novos = 7, atualizados = 0, fotos = 1), importado)
        assertEquals(listOf(moto), novo.backup.motos())
        assertEquals(listOf(leitura), novo.backup.leituras())
        assertEquals(setOf(role, oleo), novo.backup.registros().toSet())
        assertEquals(listOf(foto), novo.backup.fotos())
        assertEquals(listOf(item), novo.backup.itensDoPlano())
        assertEquals(listOf(rota), novo.backup.rotas())
        // Fora do arquivo: a rota em gravação e os pontos brutos dela (decisão #17), e os excluídos.
        assertEquals(1, novo.contar("rotas"))
        assertEquals(0, novo.contar("pontos_rota"))
        assertArrayEquals(JPEG, novo.arquivos.fotos["oleo.jpg"])
        assertEquals(1, novo.agendador.recalculos)
    }

    @Test
    fun exportarGuardaACopiaQueOBackupDoAndroidLeva() = runTest {
        val aparelho = Aparelho().cheio()
        val (arquivo, _) = aparelho.exportar()
        assertArrayEquals(arquivo, aparelho.arquivos.ultimaExportacao().readBytes())
    }

    @Test
    fun exportarSemRotasTiraOTracadoEMantemORole() = runTest {
        val (arquivo, resumo) = Aparelho().cheio().exportar(comRotas = false)
        assertEquals(0, resumo.rotas)
        assertEquals("o arquivo não leva o traçado", emptyList<Rota>(), PacoteZip.lerDados(arquivo.inputStream()).rotas)
        val novo = Aparelho()
        novo.repositorio.importar(arquivo.inputStream())
        assertTrue(novo.backup.rotas().isEmpty())
        assertNull(novo.backup.registros().single { it.id == "role" }.rotaId)
    }

    @Test
    fun importarMantemOQueFoiAlteradoDepoisNoAparelho() = runTest {
        val (arquivo, _) = Aparelho().cheio().exportar()
        val aparelho = Aparelho().cheio().apply {
            backup.gravarRegistros(listOf(oleo.copy(titulo = "Óleo e filtro", alteradoEm = 50)))
            backup.gravarMotos(listOf(moto.copy(apelido = "Antiga", alteradoEm = 1)))
        }
        val resumo = aparelho.repositorio.importar(arquivo.inputStream())
        assertEquals("Óleo e filtro", aparelho.backup.registros().single { it.id == "oleo" }.titulo)
        assertEquals("Minha moto", aparelho.backup.motos().single { it.id == "m" }.apelido)
        assertEquals(1, resumo.atualizados)
        assertEquals(0, resumo.fotos)
    }

    @Test
    fun rotaExcluidaNoAparelhoNaoVoltaComOTrajeto() = runTest {
        val (arquivo, _) = Aparelho().cheio().exportar()
        // Mesmo com o arquivo "mais novo" que a exclusão, o traçado não volta.
        val aparelho = Aparelho().cheio().apply {
            backup.gravarRotas(listOf(rota.copy(polilinha = "", excluidoEm = 2, alteradoEm = 2)))
        }
        aparelho.repositorio.importar(arquivo.inputStream())
        val depois = aparelho.backup.rotas().single { it.id == "rota" }
        assertEquals("", depois.polilinha)
        assertEquals(2L, depois.excluidoEm)
    }

    @Test
    fun falhaNoMeioNaoDeixaFotoOrfaNemMudaOBanco() = runTest {
        val (arquivo, _) = Aparelho().cheio().exportar()
        val novo = Aparelho(dao = { real ->
            object : BackupDao by real {
                override suspend fun gravarFotos(itens: List<FotoRegistro>) = throw IllegalStateException("disco cheio")
            }
        })
        try {
            novo.repositorio.importar(arquivo.inputStream())
            fail("deveria falhar")
        } catch (e: IllegalStateException) {
            assertEquals("disco cheio", e.message)
        }
        assertTrue(novo.arquivos.fotos.isEmpty())
        assertEquals("a transação volta atrás: nem a moto ficou", 0, novo.contar("motos"))
        assertEquals(0, novo.agendador.recalculos)
    }

    @Test
    fun arquivoQueNaoEhBackupNaoMudaNada() = runTest {
        val aparelho = Aparelho()
        try {
            aparelho.repositorio.importar("planilha".toByteArray().inputStream())
            fail("deveria recusar")
        } catch (e: ArquivoInvalidoException) {
            assertEquals(0, aparelho.contar("motos"))
        }
    }

    @Test
    fun poucoEspacoNoAparelhoNaoGravaAsFotos() = runTest {
        val (arquivo, _) = Aparelho().cheio().exportar()
        val novo = Aparelho().apply { arquivos.espaco = 200L * 1024 * 1024 }
        try {
            novo.repositorio.importar(arquivo.inputStream())
            fail("deveria parar por espaço")
        } catch (e: EspacoInsuficienteException) {
            assertTrue(novo.arquivos.fotos.isEmpty())
            assertEquals(0, novo.contar("motos"))
        }
    }

    @Test
    fun importarNaoDeixaDuasMotosNoGratis() = runTest {
        val (arquivo, _) = Aparelho().cheio().exportar()
        val aparelho = Aparelho().apply {
            backup.gravarMotos(listOf(Moto(id = "a", apelido = "Do aparelho", marca = "Yamaha", modelo = "Fazer")))
        }

        assertEquals(listOf("a", "m"), aparelho.repositorio.motosParaEscolher(arquivo.inputStream()).map { it.id })
        try {
            aparelho.repositorio.importar(arquivo.inputStream())
            fail("sem escolha, a importação não grava")
        } catch (e: EscolhaDeMotoNecessariaException) {
            assertEquals(listOf("a"), aparelho.backup.motos().map { it.id })
        }

        aparelho.repositorio.importar(arquivo.inputStream(), motoQueFica = "m")
        val ativas = aparelho.backup.motos().filter { it.excluidoEm == null }.map { it.id }
        assertEquals(listOf("m"), ativas)
        assertEquals(relogio.millis(), aparelho.backup.motos().single { it.id == "a" }.excluidoEm)
    }

    @Test
    fun escolherAMotoDoAparelhoNaoImportaNadaDaOutra() = runTest {
        val (arquivo, _) = Aparelho().cheio().exportar()
        val aparelho = Aparelho().apply {
            backup.gravarMotos(listOf(Moto(id = "a", apelido = "Do aparelho", marca = "Yamaha", modelo = "Fazer")))
        }
        val resumo = aparelho.repositorio.importar(arquivo.inputStream(), motoQueFica = "a")
        assertEquals(ResumoImportacao(novos = 0, atualizados = 0, fotos = 0), resumo)
        assertEquals(listOf("a"), aparelho.backup.motos().map { it.id })
        assertEquals(0, aparelho.contar("registros"))
        assertTrue(aparelho.arquivos.fotos.isEmpty())
    }

    @Test
    fun noPremiumAsDuasMotosFicam() = runTest {
        val (arquivo, _) = Aparelho().cheio().exportar()
        val aparelho = Aparelho(premium = Premium(habilitado = true)).apply {
            backup.gravarMotos(listOf(Moto(id = "a", apelido = "Do aparelho", marca = "Yamaha", modelo = "Fazer")))
        }
        assertEquals(emptyList<Moto>(), aparelho.repositorio.motosParaEscolher(arquivo.inputStream()))
        aparelho.repositorio.importar(arquivo.inputStream())
        assertEquals(setOf("a", "m"), aparelho.backup.motos().map { it.id }.toSet())
    }

    private companion object {
        val JPEG = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(),
            0xFF.toByte(), 0xE0.toByte(), 0x00, 0x04, 0x4A, 0x46,
            0xFF.toByte(), 0xDA.toByte(), 0x00, 0x02, 0x09, 0x08,
        )
    }
}
