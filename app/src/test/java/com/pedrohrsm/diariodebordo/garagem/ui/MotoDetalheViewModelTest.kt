package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.lifecycle.SavedStateHandle
import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.FakeMotoDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.garagem.dominio.Vizinha
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MotoDetalheViewModelTest {

    private val relogio = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val motos = MotoRepositorio(FakeMotoDao(), relogio)
    private val hodometro = HodometroRepositorio(FakeLeituraHodometroDao(), TransacaoFalsa(), relogio, AgendadorFalso())
    private val motoAtual = MotoAtual(motos)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun motoComKm(km: Int): Moto =
        motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883")).also {
            hodometro.registrar(it.id, km)
        }

    private fun abrir(moto: Moto) = MotoDetalheViewModel(
        SavedStateHandle(mapOf(MotoDetalheViewModel.ARG_MOTO_ID to moto.id)),
        motos,
        hodometro,
        motoAtual,
    )

    @Test
    fun mostraOKmAtualEAMotoViraAAtual() = runTest {
        motos.criar(Moto(apelido = "Antes", marca = "Honda", modelo = "CG"))
        val moto = motoComKm(23_450)
        hodometro.registrar(moto.id, 24_010)
        val vm = abrir(moto)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        val pronto = vm.estado.value as MotoDetalheEstado.Pronto
        assertEquals(24_010, pronto.kmAtual)
        assertEquals(listOf(24_010, 23_450), pronto.leituras.map { it.km })
        assertEquals(moto.id, motoAtual.moto.first()?.id)
    }

    @Test
    fun hodometroQueVoltaApontaAUltimaLeituraENaoGrava() = runTest {
        val moto = motoComKm(23_450)
        val vm = abrir(moto)
        vm.abrirHodometro()
        vm.alterarHodometro("23.000")
        vm.salvarHodometro()
        assertEquals(ErroHodometro.Menor(23_450), vm.dialogo.value?.erro)
        assertEquals(23_450, hodometro.kmAtual(moto.id))
    }

    @Test
    fun hodometroQueAvancaGravaEFechaODialogo() = runTest {
        val moto = motoComKm(23_450)
        val vm = abrir(moto)
        vm.abrirHodometro()
        vm.alterarHodometro("24.010")
        vm.salvarHodometro()
        assertNull(vm.dialogo.value)
        assertEquals(24_010, hodometro.kmAtual(moto.id))
    }

    private suspend fun leituras(moto: Moto) = hodometro.observarDaMoto(moto.id).first()

    @Test
    fun corrigirUmaLeituraDoMeioForaDoIntervaloMostraAsDuasVizinhasEAVioladaENaoGrava() = runTest {
        val moto = motoComKm(1_000)
        hodometro.registrar(moto.id, 2_000, lidaEm = 5_000_000_000_000)
        hodometro.registrar(moto.id, 3_000, lidaEm = 6_000_000_000_000)
        val vm = abrir(moto)
        val meio = leituras(moto).first { it.km == 2_000 }
        vm.abrirCorrecao(meio)
        assertEquals("2000", vm.dialogo.value?.texto)
        vm.alterarHodometro("3.500")
        vm.salvarHodometro()
        val erro = vm.dialogo.value?.erro as ErroHodometro.ForaDoIntervalo
        assertEquals(Vizinha.SEGUINTE, erro.violada)
        assertEquals(1_000, erro.anterior?.km)
        assertEquals(3_000, erro.seguinte?.km)
        assertEquals(3_000, hodometro.kmAtual(moto.id))
    }

    @Test
    fun corrigirDentroDoIntervaloFechaODialogoEMudaOKmAtual() = runTest {
        val moto = motoComKm(23_450)
        val vm = abrir(moto)
        vm.abrirCorrecao(leituras(moto).single())
        vm.alterarHodometro("2.345")
        vm.salvarHodometro()
        assertNull(vm.dialogo.value)
        assertEquals(2_345, hodometro.kmAtual(moto.id))
    }

    @Test
    fun apagarPedeConfirmacaoENadaSaiAntes() = runTest {
        val moto = motoComKm(1_000)
        hodometro.registrar(moto.id, 2_000)
        val vm = abrir(moto)
        val ultima = leituras(moto).first()
        vm.pedirApagar(ultima)
        assertEquals(ultima, vm.apagar.value?.leitura)
        assertEquals(2_000, hodometro.kmAtual(moto.id))
        vm.confirmarApagar()
        assertNull(vm.apagar.value)
        assertEquals(1_000, hodometro.kmAtual(moto.id))
    }

    @Test
    fun apagarAUnicaLeituraSemRegistrosPassaEMantemOAvisoQuandoHaRegistros() = runTest {
        val dao = FakeLeituraHodometroDao()
        val repositorio = HodometroRepositorio(dao, TransacaoFalsa(), relogio, AgendadorFalso())
        val moto = motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        repositorio.registrar(moto.id, 500)
        val vm = MotoDetalheViewModel(
            SavedStateHandle(mapOf(MotoDetalheViewModel.ARG_MOTO_ID to moto.id)),
            motos,
            repositorio,
            motoAtual,
        )
        dao.registrosComKm = 1
        vm.pedirApagar(repositorio.observarDaMoto(moto.id).first().single())
        vm.confirmarApagar()
        assertEquals(ErroApagar.UnicaComRegistrosDeKm, vm.apagar.value?.erro)
        assertTrue(vm.apagar.value?.apagando == false)
        assertEquals(500, repositorio.kmAtual(moto.id))
    }
}
