package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.lifecycle.SavedStateHandle
import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.FakeMotoDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MotoDetalheViewModelTest {

    private val relogio = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val motos = MotoRepositorio(FakeMotoDao(), relogio)
    private val hodometro = HodometroRepositorio(FakeLeituraHodometroDao(), TransacaoFalsa(), relogio)
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
        val vm = abrir(moto)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        assertEquals(23_450, (vm.estado.value as MotoDetalheEstado.Pronto).kmAtual)
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
}
