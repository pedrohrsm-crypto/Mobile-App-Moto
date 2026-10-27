package com.pedrohrsm.diariodebordo.diario.ui

import androidx.paging.testing.asSnapshot
import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.diario.data.FakeFotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.FakeRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.RegistroRepositorio
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.FakeMotoDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.garagem.data.MotoDao
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
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
class DiarioViewModelTest {

    private val relogio = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val motos = MotoRepositorio(FakeMotoDao(), relogio)
    private val dao = FakeRegistroDao()
    private val repositorio = RegistroRepositorio(
        dao,
        FakeFotoRegistroDao(),
        HodometroRepositorio(FakeLeituraHodometroDao(), TransacaoFalsa(), relogio, AgendadorFalso()),
        TransacaoFalsa(),
        relogio,
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun registro(motoId: String, tipo: TipoRegistro, dia: Int) = Registro(
        motoId = motoId,
        tipo = tipo,
        titulo = "$tipo $dia",
        dataEm = Instant.parse("2026-09-${"%02d".format(dia)}T12:00:00Z").toEpochMilli(),
    )

    @Test
    fun semMotoMostraOCaminhoDoCadastro() = runTest {
        val vm = DiarioViewModel(MotoAtual(motos), repositorio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        assertEquals(DiarioEstado.SemMoto, vm.estado.value)
    }

    @Test
    fun listaDaMotoAtualDoMaisRecenteAoMaisAntigoEFiltraPorTipo() = runTest {
        val moto = motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        val outra = "outra-moto"
        repositorio.salvar(registro(moto.id, TipoRegistro.NOTA, 1), emptyList())
        repositorio.salvar(registro(moto.id, TipoRegistro.ABASTECIMENTO, 3), emptyList())
        repositorio.salvar(registro(moto.id, TipoRegistro.NOTA, 2), emptyList())
        repositorio.salvar(registro(outra, TipoRegistro.NOTA, 4), emptyList())

        val vm = DiarioViewModel(MotoAtual(motos), repositorio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
        assertEquals(DiarioEstado.Pronto(moto, filtro = null), vm.estado.value)
        assertEquals(listOf("ABASTECIMENTO 3", "NOTA 2", "NOTA 1"), vm.registros.asSnapshot().map { it.titulo })

        vm.filtrar(TipoRegistro.NOTA)
        assertEquals(DiarioEstado.Pronto(moto, filtro = TipoRegistro.NOTA), vm.estado.value)
        assertEquals(listOf("NOTA 2", "NOTA 1"), vm.registros.asSnapshot().map { it.titulo })
    }

    @Test
    fun diarioLongoChegaEmPaginas() = runTest {
        val moto = motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        repeat(RegistroRepositorio.TAMANHO_DA_PAGINA * 4) { i ->
            repositorio.salvar(registro(moto.id, TipoRegistro.NOTA, 1 + i % 28), emptyList())
        }
        val vm = DiarioViewModel(MotoAtual(motos), repositorio)
        // Sem rolar, só a primeira carga (3 páginas, o padrão do Paging) chega à tela; o resto vem ao rolar.
        val primeiraCarga = vm.registros.asSnapshot()
        assertEquals(RegistroRepositorio.TAMANHO_DA_PAGINA * 3, primeiraCarga.size)
        val ateOFim = vm.registros.asSnapshot { scrollTo(index = RegistroRepositorio.TAMANHO_DA_PAGINA * 4 - 1) }
        assertEquals(RegistroRepositorio.TAMANHO_DA_PAGINA * 4, ateOFim.size)
    }

    @Test
    fun falhaDoBancoNaMotoAtualViraErroNaListaENaoDiarioVazio() = runTest {
        val quebrado = object : MotoDao by FakeMotoDao() {
            override fun observarAtivos(): Flow<List<Moto>> = flow { throw IllegalStateException("disco cheio") }
        }
        val vm = DiarioViewModel(MotoAtual(MotoRepositorio(quebrado, relogio)), repositorio)
        val falha = runCatching { vm.registros.asSnapshot() }.exceptionOrNull()
        assertEquals("disco cheio", falha?.message)
    }
}
