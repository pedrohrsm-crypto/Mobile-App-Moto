package com.pedrohrsm.diariodebordo.diario.data

import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.plano.data.FakeItemDoPlanoDao
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistroRepositorioTest {

    private val instante = Instant.parse("2026-10-01T12:00:00Z")
    private val relogio = Clock.fixed(instante, ZoneId.of("America/Sao_Paulo"))
    private val dao = FakeRegistroDao()
    private val fotos = FakeFotoRegistroDao()
    private val hodometro = HodometroRepositorio(FakeLeituraHodometroDao(), relogio, AgendadorFalso())
    private val repositorio =
        RegistroRepositorio(
            dao,
            fotos,
            hodometro,
            PlanoRepositorio(FakeItemDoPlanoDao(), relogio, AgendadorFalso()),
            TransacaoFalsa(),
            relogio,
        )

    private fun oleo(km: Int? = 12_000) = Registro(
        motoId = "moto",
        tipo = TipoRegistro.MANUTENCAO,
        titulo = "Troca de óleo",
        dataEm = instante.toEpochMilli(),
        hodometroKm = km,
    )

    @Test
    fun criarPreencheControleGuardaFotosEAvancaOHodometro() = runTest {
        val salvo = repositorio.salvar(oleo(), listOf("a.jpg", "b.jpg")).registro
        assertEquals(instante.toEpochMilli(), salvo.criadoEm)
        assertEquals("America/Sao_Paulo", salvo.fusoHorario)
        assertEquals(listOf("a.jpg", "b.jpg"), repositorio.fotosDo(salvo.id).map { it.arquivo })
        assertEquals(12_000, hodometro.kmAtual("moto"))
    }

    @Test
    fun editarTrocaFotosEDevolveAsRemovidas() = runTest {
        val criado = repositorio.salvar(oleo(), listOf("a.jpg", "b.jpg")).registro
        val editado = repositorio.salvar(criado.copy(titulo = "Óleo e filtro"), listOf("b.jpg", "c.jpg"))
        assertEquals(listOf("a.jpg"), editado.arquivosRemovidos)
        assertEquals(listOf("b.jpg", "c.jpg"), repositorio.fotosDo(criado.id).map { it.arquivo })
        assertEquals("Óleo e filtro", repositorio.buscar(criado.id)?.titulo)
        assertEquals(criado.criadoEm, repositorio.buscar(criado.id)?.criadoEm)
    }

    @Test
    fun registroRetroativoNaoVoltaOHodometro() = runTest {
        hodometro.registrar("moto", 20_000)
        repositorio.salvar(oleo(km = 12_000), emptyList())
        assertEquals(20_000, hodometro.kmAtual("moto"))
    }

    @Test
    fun excluirEscondeRegistroEFotosEDevolveArquivos() = runTest {
        val criado = repositorio.salvar(oleo(), listOf("a.jpg")).registro
        assertEquals(listOf("a.jpg"), repositorio.excluir(criado.id))
        assertTrue(repositorio.observarDaMoto("moto").first().isEmpty())
        assertTrue(repositorio.fotosDo(criado.id).isEmpty())
        assertEquals(1, dao.linhas.value.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun maisDeTresFotosSaoRecusadas() = runTest {
        repositorio.salvar(oleo(), listOf("1.jpg", "2.jpg", "3.jpg", "4.jpg"))
    }
}
