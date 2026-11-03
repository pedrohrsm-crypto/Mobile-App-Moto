package com.pedrohrsm.diariodebordo.garagem.data

import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.core.premium.Premium
import com.pedrohrsm.diariodebordo.garagem.dominio.EdicaoRecusadaException
import com.pedrohrsm.diariodebordo.garagem.dominio.ExclusaoRecusadaException
import com.pedrohrsm.diariodebordo.garagem.dominio.KM_MAXIMO
import com.pedrohrsm.diariodebordo.garagem.dominio.LeituraRecusadaException
import com.pedrohrsm.diariodebordo.garagem.dominio.LimiteDeMotosException
import com.pedrohrsm.diariodebordo.garagem.dominio.PontoDoHodometro
import com.pedrohrsm.diariodebordo.garagem.dominio.ValidacaoEdicao
import com.pedrohrsm.diariodebordo.garagem.dominio.ValidacaoExclusao
import com.pedrohrsm.diariodebordo.garagem.dominio.ValidacaoLeitura
import com.pedrohrsm.diariodebordo.garagem.dominio.Vizinha
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
        val cadastro =
            CadastroMoto(MotoRepositorio(FakeMotoDao(), relogio), hodometro, transacao, Premium(habilitado = false))
        val moto = cadastro.cadastrar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"), 23_450)
        assertEquals(23_450, hodometro.kmAtual(moto.id))
        assertEquals(1, transacao.execucoes)
    }

    @Test(expected = LimiteDeMotosException::class)
    fun gratisNaoCadastraSegundaMoto() = runTest {
        val cadastro =
            CadastroMoto(
                MotoRepositorio(FakeMotoDao(), relogio),
                hodometro,
                TransacaoFalsa(),
                Premium(habilitado = false),
            )
        cadastro.cadastrar(Moto(apelido = "A", marca = "Honda", modelo = "CG"), 0)
        cadastro.cadastrar(Moto(apelido = "B", marca = "Yamaha", modelo = "Fazer"), 0)
    }

    private suspend fun tresLeituras(): List<LeituraHodometro> = listOf(
        hodometro.registrar("moto", 1_000, lidaEm = 1_000),
        hodometro.registrar("moto", 2_000, lidaEm = 2_000),
        hodometro.registrar("moto", 3_000, lidaEm = 3_000),
    )

    @Test
    fun editarMarcaUmaLinhaExcluidaInsereUmaNovaNaMesmaTransacaoEMudaOKmAtual() = runTest {
        val transacao = TransacaoFalsa()
        val vigiado = FakeLeituraHodometroDao(transacao)
        val repositorio = HodometroRepositorio(vigiado, transacao, relogio, AgendadorFalso())
        val (_, _, ultima) = listOf(
            repositorio.registrar("moto", 1_000, lidaEm = 1_000),
            repositorio.registrar("moto", 2_000, lidaEm = 2_000),
            repositorio.registrar("moto", 3_000, lidaEm = 3_000),
        )
        val execucoes = transacao.execucoes

        val nova = repositorio.corrigir(ultima.id, 2_500)

        assertEquals(execucoes + 1, transacao.execucoes)
        assertEquals(emptyList<String>(), vigiado.foraDaTransacao)
        assertEquals(4, vigiado.linhas.value.size)
        val excluidas = vigiado.linhas.value.filter { it.excluidoEm != null }
        assertEquals(listOf(ultima.id), excluidas.map { it.id })
        assertEquals(instante.toEpochMilli(), excluidas.single().excluidoEm)
        assertEquals(2_500, nova.km)
        assertEquals(ultima.lidaEm, nova.lidaEm)
        assertEquals(instante.toEpochMilli(), nova.criadoEm)
        assertEquals(1, vigiado.linhas.value.count { it.id == nova.id && it.excluidoEm == null })
        assertEquals(2_500, repositorio.kmAtual("moto"))
        assertEquals(listOf(KmAtual("moto", 2_500)), repositorio.observarKmAtuais().first())
    }

    @Test
    fun editarForaDoIntervaloDasVizinhasEhRecusadoENaoGravaNada() = runTest {
        val (_, meio, _) = tresLeituras()
        val antes = dao.linhas.value
        val recusa = runCatching { hodometro.corrigir(meio.id, 3_000) }.exceptionOrNull()
        val motivo = (recusa as EdicaoRecusadaException).motivo as ValidacaoEdicao.ForaDoIntervalo
        assertEquals(Vizinha.SEGUINTE, motivo.violada)
        assertEquals(3_000, motivo.seguinte?.km)
        assertEquals(antes, dao.linhas.value)
        assertEquals(3_000, hodometro.kmAtual("moto"))
    }

    @Test
    fun editarComOMesmoKmNaoGravaNemRecalcula() = runTest {
        val (primeira, _, _) = tresLeituras()
        val recalculos = agendador.recalculos
        val devolvida = hodometro.corrigir(primeira.id, primeira.km)
        assertEquals(primeira.id, devolvida.id)
        assertEquals(3, dao.linhas.value.size)
        assertEquals(recalculos, agendador.recalculos)
    }

    @Test
    fun editarLeituraInexistenteOuJaApagadaEhRecusado() = runTest {
        val (primeira, _, _) = tresLeituras()
        hodometro.apagar(primeira.id)
        val recusa = runCatching { hodometro.corrigir(primeira.id, 500) }.exceptionOrNull()
        assertEquals(ValidacaoEdicao.NaoEncontrada, (recusa as EdicaoRecusadaException).motivo)
    }

    @Test
    fun apagarMarcaExcluidaEOKmAtualRecalcula() = runTest {
        val (_, _, ultima) = tresLeituras()
        val recalculos = agendador.recalculos
        hodometro.apagar(ultima.id)
        assertEquals(2_000, hodometro.kmAtual("moto"))
        assertEquals(listOf(ultima.id), dao.linhas.value.filter { it.excluidoEm != null }.map { it.id })
        assertEquals(3, dao.linhas.value.size)
        assertEquals(recalculos + 1, agendador.recalculos)
    }

    @Test
    fun apagarAUnicaLeituraDeMotoComRegistrosDeKmEhRecusadoMasSemRegistrosPassa() = runTest {
        val unica = hodometro.registrar("moto", 1_000)
        dao.registrosComKm = 2
        val recusa = runCatching { hodometro.apagar(unica.id) }.exceptionOrNull()
        assertEquals(ValidacaoExclusao.UnicaComRegistrosDeKm, (recusa as ExclusaoRecusadaException).motivo)
        assertEquals(1_000, hodometro.kmAtual("moto"))

        dao.registrosComKm = 0
        hodometro.apagar(unica.id)
        assertEquals(null, hodometro.kmAtual("moto"))
    }

    @Test
    fun apagarEEditarConfereAsLinhasAfetadas() = runTest {
        val (primeira, _, _) = tresLeituras()
        // Outra escrita apagou a leitura entre a validação e a marcação: o repositório não finge sucesso.
        val concorrente = object : LeituraHodometroDao by dao {
            override suspend fun marcarExcluida(id: String, quando: Long): Int = 0
        }
        val repositorio = HodometroRepositorio(concorrente, TransacaoFalsa(), relogio, AgendadorFalso())
        assertTrue(runCatching { repositorio.apagar(primeira.id) }.exceptionOrNull() is IllegalStateException)
        assertTrue(runCatching { repositorio.corrigir(primeira.id, 500) }.exceptionOrNull() is IllegalStateException)
        assertEquals(3, dao.linhas.value.count { it.excluidoEm == null })
    }

    @Test
    fun corrigirPreservaOFusoDaLeituraAntiga() = runTest {
        // O relógio do teste está em São Paulo; a leitura foi lançada em Tóquio.
        val antiga = LeituraHodometro(motoId = "moto", km = 1_000, lidaEm = 1_000, fusoHorario = "Asia/Tokyo")
        dao.inserir(antiga)
        val nova = hodometro.corrigir(antiga.id, 900)
        assertEquals("Asia/Tokyo", nova.fusoHorario)
        assertEquals(antiga.lidaEm, nova.lidaEm)
    }

    @Test
    fun editarParaOKmDeOutraLeituraQueNaoEVizinhaEhRecusadoENaoGravaNada() = runTest {
        // Retroativo do diário: pela data a ordem é 1.000, 7.000, 6.000; editar a primeira para 6.000 cabe
        // abaixo da vizinha (7.000) mas repete o km da terceira.
        val primeira = LeituraHodometro(motoId = "moto", km = 1_000, lidaEm = 1_000)
        val terceira = LeituraHodometro(motoId = "moto", km = 6_000, lidaEm = 3_000)
        listOf(primeira, LeituraHodometro(motoId = "moto", km = 7_000, lidaEm = 2_000), terceira)
            .forEach { dao.inserir(it) }
        val antes = dao.linhas.value
        val recusa = runCatching { hodometro.corrigir(primeira.id, 6_000) }.exceptionOrNull()
        assertEquals(
            ValidacaoEdicao.IgualAOutraLeitura(
                PontoDoHodometro(terceira.id, 6_000, 3_000, terceira.fusoHorario),
            ),
            (recusa as EdicaoRecusadaException).motivo,
        )
        assertEquals(antes, dao.linhas.value)
    }
}
