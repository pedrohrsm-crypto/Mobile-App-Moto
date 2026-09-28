package com.pedrohrsm.diariodebordo.garagem.ui

import com.pedrohrsm.diariodebordo.core.premium.Premium
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.FakeMotoDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GaragemViewModelTest {

    private val relogio = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val repositorio = MotoRepositorio(FakeMotoDao(), relogio)
    private val hodometro = HodometroRepositorio(FakeLeituraHodometroDao(), relogio, AgendadorFalso())

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun comecaCarregandoEMostraAsMotosDoRepositorio() = runTest {
        val viewModel = GaragemViewModel(repositorio, hodometro, Premium(habilitado = false))
        assertEquals(GaragemEstado.Carregando, viewModel.estado.value)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect {} }
        assertEquals(GaragemEstado.Pronto(emptyList()), viewModel.estado.value)

        val criada = repositorio.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "Sportster 883"))
        hodometro.registrar(criada.id, 23_450)
        assertEquals(
            GaragemEstado.Pronto(listOf(criada), mapOf(criada.id to 23_450), podeAdicionar = false),
            viewModel.estado.value,
        )
    }
}
