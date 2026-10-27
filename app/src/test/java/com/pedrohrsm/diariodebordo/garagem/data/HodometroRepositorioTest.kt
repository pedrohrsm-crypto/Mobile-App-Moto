package com.pedrohrsm.diariodebordo.garagem.data

import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.garagem.dominio.KM_MAXIMO
import com.pedrohrsm.diariodebordo.garagem.dominio.LeituraRecusadaException
import com.pedrohrsm.diariodebordo.garagem.dominio.LimiteDeMotosException
import com.pedrohrsm.diariodebordo.garagem.dominio.ValidacaoLeitura
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class HodometroRepositorioTest {

    private val instante = Instant.parse("2026-10-01T12:00:00Z")
    private val relogio = Clock.fixed(instante, ZoneId.of("America/Sao_Paulo"))
    private val dao = FakeLeituraHodometroDao()
    private val agendador = AgendadorFalso()
    private val hodometro = HodometroRepositorio(dao, TransacaoFalsa(), relogio, agendador)

    @Test
    fun registrarPreencheControleEViraKmAtual() = runTest {
        val leitura = hodometro.registrar("moto", 1_000)
        assertEquals(instante.toEpochMilli(), leitura.criadoEm)
        assertEquals("America/Sao_Paulo", leitura.fusoHorario)
        assertEquals(1_000, hodometro.kmAtual("moto"))
        assertEquals(listOf(KmAtual("moto", 1_000)), hodometro.observarKmAtuais().first())
    }

    @Test
    fun leituraMenorQueAUltimaEhRecusadaENaoGrava() = runTest {
        hodometro.registrar("moto", 10_000)
        try {
            hodometro.registrar("moto", 9_000)
            fail("aceitou km menor")
        } catch (e: LeituraRecusadaException) {
            assertEquals(ValidacaoLeitura.MenorQueAnterior(10_000), e.motivo)
        }
        assertEquals(1, dao.linhas.value.size)
    }

    @Test
    fun registroRetroativoNaoMexeNoKmAtual() = runTest {
        hodometro.registrar("moto", 10_000)
        assertFalse(hodometro.registrarSeAvancar("moto", 8_000, lidaEm = 0))
        assertFalse(hodometro.registrarSeAvancar("moto", 10_000, lidaEm = 0))
        assertTrue(hodometro.registrarSeAvancar("moto", 10_500, lidaEm = 0))
        assertEquals(10_500, hodometro.kmAtual("moto"))
        assertEquals(2, dao.linhas.value.size)
    }

    @Test
    fun cadaLeituraGravadaPedeUmRecalculoDosLembretesELeituraRecusadaNaoPede() = runTest {
        hodometro.registrar("moto", 10_000)
        assertEquals(1, agendador.recalculos)
        assertTrue(hodometro.registrarSeAvancar("moto", 10_500, lidaEm = 0))
        assertEquals(2, agendador.recalculos)
        // Retroativo e recusada: o km atual não mudou, então não há o que recalcular.
        assertFalse(hodometro.registrarSeAvancar("moto", 8_000, lidaEm = 0))
        runCatching { hodometro.registrar("moto", 9_000) }
        assertEquals(2, agendador.recalculos)
    }

    @Test
    fun leituraDoKmAtualEInsercaoFicamNaMesmaTransacao() = runTest {
        val transacao = TransacaoFalsa()
        val vigiado = FakeLeituraHodometroDao(transacao)
        val repositorio = HodometroRepositorio(vigiado, transacao, relogio, AgendadorFalso())
        // O vigia funciona: uma leitura feita fora de transação é anotada.
        vigiado.kmAtual("moto")
        assertEquals(listOf("kmAtual"), vigiado.foraDaTransacao)
        vigiado.foraDaTransacao.clear()

        repositorio.registrar("moto", 1_000)
        assertTrue(repositorio.registrarSeAvancar("moto", 1_200, lidaEm = 0))
        assertFalse(repositorio.registrarSeAvancar("moto", 900, lidaEm = 0))
        runCatching { repositorio.registrar("moto", 1_100) }
        assertEquals(emptyList<String>(), vigiado.foraDaTransacao)
        assertEquals(listOf(1_000, 1_200), vigiado.linhas.value.map { it.km })
        // E a transação fecha: depois dela, uma leitura solta volta a ser anotada.
        vigiado.kmAtual("moto")
        assertEquals(listOf("kmAtual"), vigiado.foraDaTransacao)
    }

    @Test
    fun registroDoDiarioComKmInvalidoEhRecusadoPelaMesmaRegra() = runTest {
        hodometro.registrar("moto", 1_000)
        try {
            hodometro.registrarSeAvancar("moto", KM_MAXIMO + 1, lidaEm = 0)
            fail("aceitou km acima do limite")
        } catch (e: LeituraRecusadaException) {
            assertEquals(ValidacaoLeitura.AcimaDoLimite, e.motivo)
        }
        assertEquals(1_000, hodometro.kmAtual("moto"))
    }

    @Test
    fun cadastroCriaMotoELeituraInicialNumaTransacao() = runTest {
        val transacao = TransacaoFalsa()
        val cadastro = CadastroMoto(MotoRepositorio(FakeMotoDao(), relogio), hodometro, transacao)
        val moto = cadastro.cadastrar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"), 23_450)
        assertEquals(23_450, hodometro.kmAtual(moto.id))
        assertEquals(1, transacao.execucoes)
    }

    @Test(expected = LimiteDeMotosException::class)
    fun gratisNaoCadastraSegundaMoto() = runTest {
        val cadastro = CadastroMoto(MotoRepositorio(FakeMotoDao(), relogio), hodometro, TransacaoFalsa())
        cadastro.cadastrar(Moto(apelido = "A", marca = "Honda", modelo = "CG"), 0)
        cadastro.cadastrar(Moto(apelido = "B", marca = "Yamaha", modelo = "Fazer"), 0)
    }
}
