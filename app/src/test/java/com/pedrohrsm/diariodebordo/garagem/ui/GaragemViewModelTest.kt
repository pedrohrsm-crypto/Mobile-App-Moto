package com.pedrohrsm.diariodebordo.garagem.ui

import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.FakeMotoDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoDao
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    private val hodometro = HodometroRepositorio(FakeLeituraHodometroDao(), TransacaoFalsa(), relogio)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun comecaCarregandoEMostraAsMotosDoRepositorio() = runTest {
        val viewModel = GaragemViewModel(repositorio, hodometro)
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

    @Test
    fun falhaDoBancoViraErroENaoGaragemVazia() = runTest {
        val quebrado = object : MotoDao by FakeMotoDao() {
            override fun observarAtivos(): Flow<List<Moto>> = flow { throw IllegalStateException("disco cheio") }
        }
        val viewModel = GaragemViewModel(MotoRepositorio(quebrado, relogio), hodometro)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect {} }
        assertEquals(GaragemEstado.Erro("disco cheio"), viewModel.estado.value)
    }
}
