package com.pedrohrsm.diariodebordo.plano.ui

import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.TipoOcorrencia
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.CategoriaMoto
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.FakeMotoDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.plano.data.FakeItemDoPlanoDao
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import com.pedrohrsm.diariodebordo.plano.dominio.CHAVE_TROCA_DE_OLEO
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import com.pedrohrsm.diariodebordo.plano.dominio.planoBase
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanoViewModelTest {

    private val instante = Instant.parse("2026-10-01T12:00:00Z")
    private val relogio = Clock.fixed(instante, ZoneId.of("America/Sao_Paulo"))
    private val motos = MotoRepositorio(FakeMotoDao(), relogio)
    private val hodometro = HodometroRepositorio(FakeLeituraHodometroDao(), TransacaoFalsa(), relogio, AgendadorFalso())
    private val dao = FakeItemDoPlanoDao()
    private val plano = PlanoRepositorio(dao, TransacaoFalsa(), relogio, AgendadorFalso())

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.abrir(): PlanoViewModel {
        val vm = PlanoViewModel(MotoAtual(motos), plano, hodometro, relogio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    private suspend fun motoStreetComKm(km: Int): Moto =
        motos.criar(Moto(apelido = "Bandida", marca = "Honda", modelo = "CB 500", categoria = CategoriaMoto.STREET))
            .also { hodometro.registrar(it.id, km) }

    @Test
    fun semMotoMostraOCaminhoParaOCadastro() = runTest {
        assertEquals(PlanoEstado.SemMoto, abrir().estado.value)
    }

    @Test
    fun abrirCriaOPlanoBaseEAsPendenciasDasOcorrenciasEMostraTudo() = runTest {
        val moto = motoStreetComKm(10_000)
        dao.registros.value = listOf(
            Registro(
                motoId = moto.id,
                tipo = TipoRegistro.OCORRENCIA,
                titulo = "Tombo",
                dataEm = instante.toEpochMilli(),
                ocorrencia = TipoOcorrencia.QUEDA,
            ),
        )
        val pronto = abrir().estado.value as PlanoEstado.Pronto
        assertEquals(10_000, pronto.kmAtual)
        assertEquals(planoBase(CategoriaMoto.STREET).size, pronto.itens.size)
        assertEquals(4, pronto.pendencias.size)
        assertTrue(pronto.itens.all { it.situacao.urgencia == Urgencia.EM_DIA })
    }

    @Test
    fun hodometroQueAvancaPassaOsItensParaVencidoNaFrenteDaLista() = runTest {
        val moto = motoStreetComKm(10_000)
        val vm = abrir()
        hodometro.registrar(moto.id, 15_100)
        val pronto = vm.estado.value as PlanoEstado.Pronto
        val urgencias = pronto.itens.map { it.situacao.urgencia }
        assertEquals(Urgencia.VENCIDO, urgencias.first())
        assertEquals(urgencias.sortedDescending(), urgencias)
        assertTrue(
            pronto.itens.any {
                it.item.chave == CHAVE_TROCA_DE_OLEO && it.situacao.urgencia == Urgencia.VENCIDO
            },
        )
    }

    @Test
    fun bancoQueFalhaViraEstadoDeErroNuncaListaVazia() = runTest {
        motoStreetComKm(10_000)
        dao.falha = IllegalStateException("disco cheio")
        val estado = abrir().estado.value
        assertEquals(PlanoEstado.Erro("disco cheio"), estado)
    }

    @Test
    fun marcarFeitoRecomecaAContaDoKmAtual() = runTest {
        val moto = motoStreetComKm(15_100)
        val vm = abrir()
        val oleo = (vm.estado.value as PlanoEstado.Pronto).itens.single { it.item.chave == CHAVE_TROCA_DE_OLEO }
        // O km sobe depois que o plano abriu: 'Fiz agora' conta do km de agora, não do que o item guardava.
        hodometro.registrar(moto.id, 15_600)
        vm.marcarFeito(oleo.item)
        val depois = (vm.estado.value as PlanoEstado.Pronto).itens.single { it.item.chave == CHAVE_TROCA_DE_OLEO }
        assertEquals(15_600, depois.item.ultimaVezKm)
        assertEquals(moto.id, depois.item.motoId)
        assertNotNull(depois.item.feitoEm)
        assertNull(vm.falha.value)
    }

    @Test
    fun falhaAoGravarApareceEmFalhaEPodeSerDispensada() = runTest {
        motoStreetComKm(15_100)
        val vm = abrir()
        val oleo = (vm.estado.value as PlanoEstado.Pronto).itens.single { it.item.chave == CHAVE_TROCA_DE_OLEO }
        dao.falha = IllegalStateException("sem espaço")
        vm.marcarFeito(oleo.item)
        assertEquals("sem espaço", vm.falha.value)
        vm.limparFalha()
        assertNull(vm.falha.value)
    }

    @Test
    fun resolverPendenciaATiraDaLista() = runTest {
        val moto = motoStreetComKm(10_000)
        dao.registros.value = listOf(
            Registro(
                motoId = moto.id,
                tipo = TipoRegistro.OCORRENCIA,
                titulo = "Barulho",
                dataEm = instante.toEpochMilli(),
                ocorrencia = TipoOcorrencia.BARULHO,
            ),
        )
        val vm = abrir()
        val pendencia = (vm.estado.value as PlanoEstado.Pronto).pendencias.first()
        vm.resolver(pendencia.id)
        val depois = (vm.estado.value as PlanoEstado.Pronto).pendencias
        assertEquals(2, depois.size)
        assertTrue(depois.none { it.id == pendencia.id })
    }
}
