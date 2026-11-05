package com.pedrohrsm.diariodebordo.plano.ui

import androidx.lifecycle.SavedStateHandle
import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.FakeMotoDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.plano.data.FakeItemDoPlanoDao
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import com.pedrohrsm.diariodebordo.plano.dominio.CampoItem
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItemPlanoFormViewModelTest {

    private val relogio = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val motos = MotoRepositorio(FakeMotoDao(), relogio)
    private val dao = FakeItemDoPlanoDao()
    private val agendador = AgendadorFalso()
    private val plano = PlanoRepositorio(dao, TransacaoFalsa(), relogio, agendador)
    private val hodometro = HodometroRepositorio(FakeLeituraHodometroDao(), TransacaoFalsa(), relogio, agendador, plano)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun moto(km: Int = 7_000): Moto =
        motos.criar(Moto(apelido = "Bandida", marca = "Honda", modelo = "CB 500"))
            .also { hodometro.registrar(it.id, km) }

    private fun abrir(itemId: String? = null) = ItemPlanoFormViewModel(
        SavedStateHandle(if (itemId == null) emptyMap() else mapOf(ItemPlanoFormViewModel.ARG_ITEM_ID to itemId)),
        plano,
        hodometro,
        MotoAtual(motos),
        relogio,
    )

    @Test
    fun itemNovoSemNomeNemIntervaloApontaOsCamposEnaoGrava() = runTest {
        moto()
        val vm = abrir()
        vm.salvar()
        assertEquals(setOf(CampoItem.NOME, CampoItem.INTERVALO), vm.estado.value.invalidos)
        assertTrue(dao.linhas.value.isEmpty())
        assertFalse(vm.estado.value.concluido)
    }

    @Test
    fun itemNovoGravaComOKmAtualComoPontoDePartida() = runTest {
        val moto = moto(km = 7_000)
        val vm = abrir()
        vm.alterar { it.copy(nome = " Cabo do acelerador ", km = "5.000", dias = "365") }
        vm.salvar()
        val item = dao.linhas.value.single()
        assertEquals("Cabo do acelerador", item.nome)
        assertEquals(5_000, item.intervaloKm)
        assertEquals(365, item.intervaloDias)
        assertEquals(7_000, item.ultimaVezKm)
        assertEquals(moto.id, item.motoId)
        assertTrue(vm.estado.value.concluido)
    }

    @Test
    fun editarCarregaOsCamposESalvaOsNovosIntervalos() = runTest {
        val moto = moto()
        val existente = plano.salvar(
            ItemDoPlano(motoId = moto.id, nome = "Velas", intervaloKm = 10_000),
            kmAtual = 7_000,
        )
        val vm = abrir(existente.id)
        assertEquals("Velas", vm.estado.value.nome)
        assertEquals("10000", vm.estado.value.km)
        vm.alterar { it.copy(km = "8.000") }
        vm.salvar()
        assertEquals(8_000, dao.linhas.value.single().intervaloKm)
        assertTrue(vm.estado.value.concluido)
    }

    @Test
    fun excluirTiraOItemDoPlano() = runTest {
        val moto = moto()
        val existente = plano.salvar(
            ItemDoPlano(motoId = moto.id, nome = "Velas", intervaloKm = 10_000),
            kmAtual = 7_000,
        )
        val vm = abrir(existente.id)
        vm.excluir()
        assertTrue(vm.estado.value.concluido)
        assertTrue(plano.abertos().isEmpty())
    }

    @Test
    fun falhaDoBancoAoSalvarApareceNaTelaENaoConclui() = runTest {
        moto()
        val vm = abrir()
        vm.alterar { it.copy(nome = "Velas", km = "10000") }
        dao.falha = IllegalStateException("disco cheio")
        vm.salvar()
        assertEquals("disco cheio", vm.estado.value.erro)
        assertFalse(vm.estado.value.concluido)
        assertFalse(vm.estado.value.salvando)
        assertNull(vm.estado.value.invalidos.firstOrNull())
    }

    /** Óleo a cada 3.000 km, feito aos 6.000 em 1-Set às 15h; criado antes, em 1-Ago (outra data, outro km). */
    private suspend fun oleoFeito(motoId: String): ItemDoPlano = ItemDoPlano(
        id = "oleo",
        motoId = motoId,
        nome = "Óleo",
        intervaloKm = 3_000,
        ultimaVezKm = 6_000,
        ultimaVezEm = Instant.parse("2026-09-01T18:00:00Z").toEpochMilli(),
        criadoEm = Instant.parse("2026-08-01T18:00:00Z").toEpochMilli(),
    ).also { dao.inserir(it) }

    @Test
    fun editarCarregaOFeitoDoItem() = runTest {
        val moto = moto()
        val vm = abrir(oleoFeito(moto.id).id)
        assertEquals("6000", vm.estado.value.feitoKm)
        assertEquals(LocalDate.of(2026, 9, 1), vm.estado.value.feitoDia)
        assertEquals(7_000, vm.estado.value.kmAtual)
    }

    @Test
    fun itemNovoNaoMostraOFeito() = runTest {
        moto()
        assertNull(abrir().estado.value.feitoDia)
    }

    @Test
    fun feitoEditadoGravaKmEDataComAHoraDeAntes() = runTest {
        val moto = moto()
        val vm = abrir(oleoFeito(moto.id).id)
        vm.alterar { it.copy(feitoKm = "6.500", feitoDia = LocalDate.of(2026, 9, 15)) }
        vm.salvar()
        val salvo = dao.linhas.value.single()
        assertEquals(6_500, salvo.ultimaVezKm)
        assertEquals(Instant.parse("2026-09-15T18:00:00Z").toEpochMilli(), salvo.ultimaVezEm)
        assertTrue(vm.estado.value.concluido)
    }

    @Test
    fun feitoComKmAcimaDoAtualApontaOCampoENaoGrava() = runTest {
        val moto = moto()
        val oleo = oleoFeito(moto.id)
        val vm = abrir(oleo.id)
        vm.alterar { it.copy(feitoKm = "7.001") }
        vm.salvar()
        assertEquals(setOf(CampoItem.FEITO_KM), vm.estado.value.invalidos)
        assertEquals(listOf(oleo), dao.linhas.value)
        assertFalse(vm.estado.value.concluido)
    }

    @Test
    fun feitoComDataDepoisDeHojeApontaOCampoENaoGrava() = runTest {
        val moto = moto()
        val oleo = oleoFeito(moto.id)
        val vm = abrir(oleo.id)
        vm.alterar { it.copy(feitoDia = LocalDate.of(2026, 10, 2)) }
        vm.salvar()
        assertEquals(setOf(CampoItem.FEITO_DATA), vm.estado.value.invalidos)
        assertEquals(listOf(oleo), dao.linhas.value)
    }
}
