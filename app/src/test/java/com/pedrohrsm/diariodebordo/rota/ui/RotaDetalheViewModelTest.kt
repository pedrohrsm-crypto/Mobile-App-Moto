package com.pedrohrsm.diariodebordo.rota.ui

import androidx.lifecycle.SavedStateHandle
import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.diario.data.FakeFotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.FakeRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.RegistroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.localizacao.Ponto
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import com.pedrohrsm.diariodebordo.rota.data.FakePontoDeRotaDao
import com.pedrohrsm.diariodebordo.rota.data.FakeRotaDao
import com.pedrohrsm.diariodebordo.rota.data.Rota
import com.pedrohrsm.diariodebordo.rota.data.RotaRepositorio
import com.pedrohrsm.diariodebordo.rota.dominio.Polilinha
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RotaDetalheViewModelTest {

    private val relogio = Clock.fixed(Instant.parse("2026-10-04T09:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val rotaDao = FakeRotaDao()
    private val transacao = TransacaoFalsa()
    private val rotas = RotaRepositorio(
        rotaDao,
        FakePontoDeRotaDao(),
        RegistroRepositorio(
            FakeRegistroDao(),
            FakeFotoRegistroDao(),
            HodometroRepositorio(FakeLeituraHodometroDao(), transacao, relogio, AgendadorFalso()),
            transacao,
            relogio,
        ),
        transacao,
        relogio,
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun TestScope.abrir(rota: Rota): RotaDetalheViewModel {
        rotaDao.inserir(rota)
        val vm = RotaDetalheViewModel(SavedStateHandle(mapOf(RotaDetalheViewModel.ARG_ROTA_ID to rota.id)), rotas)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        return vm
    }

    @Test
    fun rotaSalvaChegaComOTracadoDecodificado() = runTest {
        val pontos = listOf(Ponto(-28.0, -49.6, 0), Ponto(-28.01, -49.59, 0))
        val rota = Rota(motoId = "m", iniciadaEm = 1, polilinha = Polilinha.codificar(pontos), emGravacao = false)
        val estado = abrir(rota).estado.value
        assertTrue(estado.toString(), estado is RotaDetalheEstado.Pronto)
        assertEquals(2, (estado as RotaDetalheEstado.Pronto).pontos.size)
        assertEquals(-28.01, estado.pontos.last().latitude, 1e-5)
    }

    /** O defeito da revisão: a polilinha corrompida virava um quadro vazio, sem aviso. */
    @Test
    fun polilinhaCorrompidaViraErroENaoTracadoVazio() = runTest {
        val rota = Rota(motoId = "m", iniciadaEm = 1, polilinha = "_p~iF~ps|U_ulL", emGravacao = false)
        val estado = abrir(rota).estado.value
        assertTrue(estado.toString(), estado is RotaDetalheEstado.Erro)
    }
}
