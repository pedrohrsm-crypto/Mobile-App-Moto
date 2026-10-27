package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.lifecycle.SavedStateHandle
import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.garagem.data.CadastroMoto
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.FakeMotoDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.garagem.dominio.CampoMoto
import com.pedrohrsm.diariodebordo.garagem.dominio.LIMITE_MOTOS_GRATIS
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class MotoFormViewModelTest {

    private val relogio = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val motos = MotoRepositorio(FakeMotoDao(), relogio)
    private val hodometro = HodometroRepositorio(FakeLeituraHodometroDao(), TransacaoFalsa(), relogio)
    private val cadastro = CadastroMoto(motos, hodometro, TransacaoFalsa())
    private val motoAtual = MotoAtual(motos)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun novo(motoId: String? = null) = MotoFormViewModel(
        SavedStateHandle(listOfNotNull(motoId?.let { MotoFormViewModel.ARG_MOTO_ID to it }).toMap()),
        motos,
        cadastro,
        motoAtual,
        relogio,
    )

    private fun MotoFormViewModel.preencher(apelido: String = "Bandida", km: String = "23.450") =
        alterar { it.copy(apelido = apelido, marca = "Harley-Davidson", modelo = "883", kmInicial = km) }

    @Test
    fun cadastroGravaMotoEHodometroESelecionaAMoto() = runTest {
        val vm = novo()
        vm.preencher()
        vm.salvar()
        val salvoId = vm.estado.value.salvoId
        assertEquals(23_450, hodometro.kmAtual(salvoId!!))
        assertEquals(salvoId, motoAtual.moto.first()?.id)
    }

    @Test
    fun segundaMotoNoGratisViraAvisoDoLimiteENaoGrava() = runTest {
        motos.criar(Moto(apelido = "Primeira", marca = "Honda", modelo = "CG"))
        val vm = novo()
        vm.preencher(apelido = "Segunda")
        vm.salvar()
        assertEquals(ErroMotoForm.LimiteGratis(LIMITE_MOTOS_GRATIS), vm.estado.value.erro)
        assertNull(vm.estado.value.salvoId)
        assertEquals(1, motos.observarAtivos().first().size)
    }

    @Test
    fun camposInvalidosNaoChegamAoBanco() = runTest {
        val vm = novo()
        vm.preencher(apelido = " ", km = "12,5")
        vm.salvar()
        assertEquals(setOf(CampoMoto.APELIDO, CampoMoto.KM), vm.estado.value.invalidos)
        assertTrue(motos.observarAtivos().first().isEmpty())
    }

    @Test
    fun edicaoMudaOsDadosSemCriarLeitura() = runTest {
        val moto = cadastro.cadastrar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"), 23_450)
        val vm = novo(moto.id)
        assertEquals("Bandida", vm.estado.value.apelido)
        vm.alterar { it.copy(apelido = "Velha de guerra") }
        vm.salvar()
        assertEquals("Velha de guerra", motos.observar(moto.id).first()?.apelido)
        assertEquals(23_450, hodometro.kmAtual(moto.id))
    }
}
