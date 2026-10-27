package com.pedrohrsm.diariodebordo.rota.ui

import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.diario.data.FakeFotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.FakeRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.RegistroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.FakeMotoDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.garagem.data.MotoDao
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.localizacao.Ponto
import com.pedrohrsm.diariodebordo.rota.data.FakePontoDeRotaDao
import com.pedrohrsm.diariodebordo.rota.data.FakeRotaDao
import com.pedrohrsm.diariodebordo.rota.data.RotaRepositorio
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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

class StatusCapturaFalso : StatusCaptura {
    override val ativo = MutableStateFlow(false)
    override val falha = MutableStateFlow<FalhaRota?>(null)
    override fun limparFalha() {
        falha.value = null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RotasViewModelTest {

    private val relogio = Clock.fixed(Instant.parse("2026-10-04T09:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val motoDao = FakeMotoDao()
    private val motos = MotoRepositorio(motoDao, relogio)
    private val rotaDao = FakeRotaDao()
    private val pontoDao = FakePontoDeRotaDao()
    private val transacao = TransacaoFalsa()
    private val registros = RegistroRepositorio(
        FakeRegistroDao(),
        FakeFotoRegistroDao(),
        HodometroRepositorio(FakeLeituraHodometroDao(), transacao, relogio),
        transacao,
        relogio,
    )
    private val rotas = RotaRepositorio(rotaDao, pontoDao, registros, transacao, relogio)
    private val captura = StatusCapturaFalso()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun novo(motoAtual: MotoAtual = MotoAtual(motos)) = RotasViewModel(motoAtual, rotas, captura)

    /** Coleta os StateFlows enquanto o teste roda (eles só andam com alguém inscrito). */
    private fun TestScope.observar(vm: RotasViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.falha.collect {} }
    }

    @Test
    fun semMotoLevaAoCadastro() = runTest {
        val vm = novo()
        observar(vm)
        assertEquals(RotasEstado.SemMoto, vm.estado.value)
    }

    @Test
    fun falhaDoBancoViraErroENaoListaVazia() = runTest {
        val quebrado = object : MotoDao by FakeMotoDao() {
            override fun observarAtivos(): Flow<List<Moto>> = flow { throw IllegalStateException("disco cheio") }
        }
        val vm = novo(MotoAtual(MotoRepositorio(quebrado, relogio)))
        observar(vm)
        assertEquals(RotasEstado.Erro("disco cheio"), vm.estado.value)
    }

    @Test
    fun gravacaoSemServicoApareceComoInterrompidaESalvaOQueChegou() = runTest {
        val moto = motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        val rota = rotas.iniciar(moto.id)
        rotas.adicionarPonto(rota.id, Ponto(-23.50, -46.6, relogio.millis(), 5f))
        rotas.adicionarPonto(rota.id, Ponto(-23.51, -46.6, relogio.millis() + 60_000, 5f))
        val vm = novo()
        observar(vm)

        val gravacao = (vm.estado.value as RotasEstado.Pronto).gravacao
        assertEquals(Gravacao(rota, pontos = 2, capturando = false), gravacao)

        vm.salvarInterrompida(rota.id)
        val pronto = vm.estado.value as RotasEstado.Pronto
        assertEquals(null, pronto.gravacao)
        assertEquals(listOf(rota.id), pronto.rotas.map { it.id })
        assertTrue(pontoDao.linhas.value.isEmpty())
    }

    @Test
    fun servicoAtivoMostraCapturando() = runTest {
        val moto = motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        rotas.iniciar(moto.id)
        captura.ativo.value = true
        val vm = novo()
        observar(vm)
        assertEquals(true, (vm.estado.value as RotasEstado.Pronto).gravacao?.capturando)
    }

    /** Permissão negada: aviso, nenhuma rota criada e a tela continua de pé. */
    @Test
    fun negarALocalizacaoAvisaENaoCriaRota() = runTest {
        motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        val vm = novo()
        observar(vm)
        vm.localNegado()
        assertEquals(FalhaRota.SemPermissao, vm.falha.value)
        assertTrue(rotaDao.linhas.value.isEmpty())
        assertTrue(vm.estado.value is RotasEstado.Pronto)
        vm.limparFalha()
        assertEquals(null, vm.falha.value)
    }

    @Test
    fun falhaDoServicoChegaNaTela() = runTest {
        motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        val vm = novo()
        observar(vm)
        captura.falha.value = FalhaRota.Gps("tempo esgotado")
        assertEquals(FalhaRota.Gps("tempo esgotado"), vm.falha.value)
        vm.limparFalha()
        assertEquals(null, captura.falha.value)
    }
}
