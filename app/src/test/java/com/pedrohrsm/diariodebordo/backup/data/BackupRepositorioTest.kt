package com.pedrohrsm.diariodebordo.backup.data

import com.pedrohrsm.diariodebordo.backup.dominio.ArquivoInvalidoException
import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistro
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import com.pedrohrsm.diariodebordo.rota.data.Rota
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupRepositorioTest {

    private val relogio = Clock.fixed(Instant.parse("2026-12-01T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))

    private inner class Aparelho {
        val dao = FakeBackupDao()
        val arquivos = ArquivosFalsos(Files.createTempDirectory("backup").toFile())
        val agendador = AgendadorFalso()
        val repositorio = BackupRepositorio(dao, arquivos, TransacaoFalsa(), agendador, relogio)
    }

    /** Aparelho com uma moto, um rolê com rota, um registro com foto e um registro excluído. */
    private fun aparelhoCheio() = Aparelho().apply {
        dao.motos = listOf(
            Moto(id = "m", apelido = "Minha moto", marca = "Honda", modelo = "CG 160", alteradoEm = 10),
            Moto(id = "velha", apelido = "Vendida", marca = "Honda", modelo = "CG", excluidoEm = 5),
        )
        dao.leituras = listOf(LeituraHodometro(id = "l", motoId = "m", km = 15_000, alteradoEm = 10))
        dao.itens =
            listOf(ItemDoPlano(id = "i", motoId = "m", nome = "Troca de óleo", intervaloKm = 5_000, alteradoEm = 10))
        dao.rotas =
            listOf(Rota(id = "rota", motoId = "m", iniciadaEm = 1, emGravacao = false, polilinha = "_p~iF~ps|U"))
        dao.registros = listOf(
            Registro(
                id = "role",
                motoId = "m",
                tipo = TipoRegistro.ROLE,
                titulo = "Rolê",
                dataEm = 1,
                rotaId = "rota",
                alteradoEm = 10,
            ),
            Registro(
                id = "oleo",
                motoId = "m",
                tipo = TipoRegistro.MANUTENCAO,
                titulo = "Óleo",
                dataEm = 2,
                alteradoEm = 10,
            ),
            Registro(
                id = "apagado",
                motoId = "m",
                tipo = TipoRegistro.NOTA,
                titulo = "Segredo",
                dataEm = 3,
                excluidoEm = 4,
            ),
        )
        dao.fotos =
            listOf(FotoRegistro(id = "f", registroId = "oleo", arquivo = "oleo.jpg", ordem = 0, alteradoEm = 10))
        arquivos.fotos["oleo.jpg"] = byteArrayOf(9, 8, 7)
    }

    private suspend fun Aparelho.exportar(comRotas: Boolean = true): Pair<ByteArray, ResumoExportacao> {
        val saida = ByteArrayOutputStream()
        val resumo = repositorio.exportar(saida, comRotas)
        return saida.toByteArray() to resumo
    }

    @Test
    fun exportarEImportarNoutroAparelhoRecuperaTudo() = runTest {
        val (arquivo, resumo) = aparelhoCheio().exportar()
        assertEquals(ResumoExportacao(motos = 1, registros = 2, fotos = 1, rotas = 1, fotosFaltando = 0), resumo)

        val novo = Aparelho()
        val importado = novo.repositorio.importar(arquivo.inputStream())

        assertEquals(ResumoImportacao(novos = 7, atualizados = 0, fotos = 1), importado)
        assertEquals(listOf("m"), novo.dao.motos.map { it.id })
        assertEquals(setOf("role", "oleo"), novo.dao.registros.map { it.id }.toSet())
        assertEquals("rota", novo.dao.registros.single { it.id == "role" }.rotaId)
        assertEquals(15_000, novo.dao.leituras.single().km)
        assertEquals("Troca de óleo", novo.dao.itens.single().nome)
        assertEquals("_p~iF~ps|U", novo.dao.rotas.single().polilinha)
        assertArrayEquals(byteArrayOf(9, 8, 7), novo.arquivos.fotos["oleo.jpg"])
        assertEquals(1, novo.agendador.recalculos)
    }

    @Test
    fun exportarGuardaACopiaQueOBackupDoAndroidLeva() = runTest {
        val aparelho = aparelhoCheio()
        val (arquivo, _) = aparelho.exportar()
        assertArrayEquals(arquivo, aparelho.arquivos.ultimaExportacao().readBytes())
    }

    @Test
    fun exportarSemRotasTiraOTracadoEMantemORole() = runTest {
        val (arquivo, resumo) = aparelhoCheio().exportar(comRotas = false)
        assertEquals(0, resumo.rotas)
        val novo = Aparelho()
        novo.repositorio.importar(arquivo.inputStream())
        assertTrue(novo.dao.rotas.isEmpty())
        assertNull(novo.dao.registros.single { it.id == "role" }.rotaId)
    }

    @Test
    fun importarMantemOQueFoiAlteradoDepoisNoAparelho() = runTest {
        val (arquivo, _) = aparelhoCheio().exportar()
        val aparelho = aparelhoCheio().apply {
            dao.registros =
                dao.registros.map { if (it.id == "oleo") it.copy(titulo = "Óleo e filtro", alteradoEm = 50) else it }
            dao.motos = dao.motos.map { if (it.id == "m") it.copy(apelido = "Antiga", alteradoEm = 1) else it }
        }
        val resumo = aparelho.repositorio.importar(arquivo.inputStream())
        assertEquals("Óleo e filtro", aparelho.dao.registros.single { it.id == "oleo" }.titulo)
        assertEquals("Minha moto", aparelho.dao.motos.single { it.id == "m" }.apelido)
        assertEquals(1, resumo.atualizados)
        assertEquals(0, resumo.fotos)
    }

    @Test
    fun falhaNoMeioNaoDeixaFotoOrfa() = runTest {
        val (arquivo, _) = aparelhoCheio().exportar()
        val novo = Aparelho().apply { dao.falhaAoGravarFotos = IllegalStateException("disco cheio") }
        try {
            novo.repositorio.importar(arquivo.inputStream())
            fail("deveria falhar")
        } catch (e: IllegalStateException) {
            assertEquals("disco cheio", e.message)
        }
        assertTrue(novo.arquivos.fotos.isEmpty())
        assertEquals(0, novo.agendador.recalculos)
    }

    @Test(expected = ArquivoInvalidoException::class)
    fun arquivoQueNaoEhBackupNaoMudaNada() = runTest {
        Aparelho().repositorio.importar("planilha".toByteArray().inputStream())
    }
}
