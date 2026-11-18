package com.pedrohrsm.diariodebordo.rota.ui

import androidx.lifecycle.SavedStateHandle
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.diario.data.FakeFotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.FakeRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.RegistroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.localizacao.Ponto
import com.pedrohrsm.diariodebordo.plano.data.planoFalso
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import com.pedrohrsm.diariodebordo.rota.data.FakePontoDeRotaDao
import com.pedrohrsm.diariodebordo.rota.data.FakePrivacidadeDasRotas
import com.pedrohrsm.diariodebordo.rota.data.FakeRotaDao
import com.pedrohrsm.diariodebordo.rota.data.Rota
import com.pedrohrsm.diariodebordo.rota.data.RotaRepositorio
import com.pedrohrsm.diariodebordo.rota.dominio.Polilinha
import com.pedrohrsm.diariodebordo.rota.dominio.PontaDaRota
import com.pedrohrsm.diariodebordo.rota.dominio.ZonaDePrivacidade
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** #78: a zona marcada sem GPS, a partir da ponta de uma rota já gravada. */
@OptIn(ExperimentalCoroutinesApi::class)
class ZonaFormViewModelTest {

    private val relogio = Clock.fixed(Instant.parse("2026-11-18T09:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val rotaDao = FakeRotaDao()
    private val transacao = TransacaoFalsa()
    private val privacidade = FakePrivacidadeDasRotas()
    private val rotas = RotaRepositorio(
        rotaDao,
        FakePontoDeRotaDao(),
        RegistroRepositorio(
            FakeRegistroDao(),
            FakeFotoRegistroDao(),
            HodometroRepositorio(
                FakeLeituraHodometroDao(),
                transacao,
                relogio,
                AgendadorFalso(),
                planoFalso(relogio, transacao),
            ),
            transacao,
            relogio,
        ),
        transacao,
        relogio,
    )
    private val pontos = listOf(Ponto(-27.5, -48.5, 0), Ponto(-27.51, -48.49, 0), Ponto(-27.52, -48.47, 0))

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun nova(rota: Rota, ponta: PontaDaRota) = ZonaFormViewModel(
        SavedStateHandle(mapOf(ZonaFormViewModel.ARG_ROTA_ID to rota.id, ZonaFormViewModel.ARG_PONTA to ponta.name)),
        privacidade,
        rotas,
    )

    private suspend fun gravada(polilinha: String): Rota =
        Rota(motoId = "m", iniciadaEm = 1, polilinha = polilinha, emGravacao = false).also { rotaDao.inserir(it) }

    @Test
    fun zonaNovaTemOCentroNaPontaEscolhidaDaRota() = runTest {
        val vm = nova(gravada(Polilinha.codificar(pontos)), PontaDaRota.FIM)
        vm.alterar { it.copy(nome = " Casa ", raioM = 1_000) }
        vm.salvar()
        val zona = privacidade.estado.value.zonas.single()
        assertEquals("Casa", zona.nome)
        assertEquals(1_000, zona.raioM)
        assertEquals(-27.52, zona.latitude, 1e-5)
        assertEquals(-48.47, zona.longitude, 1e-5)
        assertTrue(vm.estado.value.concluido)
    }

    @Test
    fun zonaSemNomeNaoESalva() = runTest {
        val vm = nova(gravada(Polilinha.codificar(pontos)), PontaDaRota.INICIO)
        vm.salvar()
        assertTrue(vm.estado.value.nomeInvalido)
        assertEquals(emptyList<ZonaDePrivacidade>(), privacidade.estado.value.zonas)
    }

    @Test
    fun rotaSemTracadoNaoDaCentroParaAZona() = runTest {
        val vm = nova(gravada(""), PontaDaRota.INICIO)
        assertEquals(R.string.zona_sem_tracado, vm.estado.value.indisponivel)
    }

    @Test
    fun editarMudaNomeERaioEMantemOCentro() = runTest {
        val casa = ZonaDePrivacidade(id = "z", nome = "Casa", latitude = -27.5, longitude = -48.5, raioM = 500)
        privacidade.salvarZona(casa)
        val vm = ZonaFormViewModel(SavedStateHandle(mapOf(ZonaFormViewModel.ARG_ZONA_ID to "z")), privacidade, rotas)
        vm.alterar { it.copy(nome = "Trabalho", raioM = 200) }
        vm.salvar()
        assertEquals(listOf(casa.copy(nome = "Trabalho", raioM = 200)), privacidade.estado.value.zonas)
    }
}
