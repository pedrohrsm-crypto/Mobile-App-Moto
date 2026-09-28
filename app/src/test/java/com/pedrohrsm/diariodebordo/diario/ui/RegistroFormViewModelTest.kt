package com.pedrohrsm.diariodebordo.diario.ui

import androidx.lifecycle.SavedStateHandle
import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.core.preferencias.FakePreferencias
import com.pedrohrsm.diariodebordo.diario.data.FakeFotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.FakeRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.RegistroRepositorio
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.diario.dominio.CampoRegistro
import com.pedrohrsm.diariodebordo.diario.fotos.GuardaFotos
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.FakeMotoDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.plano.data.FakeItemDoPlanoDao
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GuardaFotosFalsa : GuardaFotos {
    val guardadas = mutableListOf<String>()
    val apagadas = mutableListOf<String>()
    override suspend fun importar(uri: String): String = "foto-${guardadas.size + 1}.jpg".also { guardadas += it }
    override fun apagar(arquivo: String): Boolean {
        apagadas += arquivo
        return true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RegistroFormViewModelTest {

    private val relogio = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val motos = MotoRepositorio(FakeMotoDao(), relogio)
    private val motoAtual = MotoAtual(motos, FakePreferencias())
    private val registros = FakeRegistroDao()
    private val hodometro = HodometroRepositorio(FakeLeituraHodometroDao(), relogio)
    private val plano = PlanoRepositorio(FakeItemDoPlanoDao(), relogio)
    private val repositorio = RegistroRepositorio(
        registros,
        FakeFotoRegistroDao(),
        hodometro,
        plano,
        TransacaoFalsa(),
        relogio,
    )
    private val fotos = GuardaFotosFalsa()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun novo(args: Map<String, Any?> = emptyMap()) =
        RegistroFormViewModel(SavedStateHandle(args), repositorio, fotos, motoAtual, plano, hodometro, relogio)

    @Test
    fun registroNovoUsaAMotoAtualEOTipoPedido() = runTest {
        val moto = motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        val vm = novo(mapOf(RegistroFormViewModel.ARG_TIPO to "ABASTECIMENTO"))
        assertEquals(moto.id, vm.estado.value.motoId)
        assertEquals(TipoRegistro.ABASTECIMENTO, vm.estado.value.tipo)
        assertEquals(relogio.millis(), vm.estado.value.dataEm)
    }

    @Test
    fun salvarSemTituloApontaOCampo() = runTest {
        motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        val vm = novo()
        vm.salvar()
        assertEquals(setOf(CampoRegistro.TITULO), vm.estado.value.invalidos)
        assertTrue(registros.linhas.value.isEmpty())
    }

    @Test
    fun salvarGravaComFotosEConclui() = runTest {
        val moto = motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        val vm = novo()
        vm.alterar { it.copy(titulo = "Troca de óleo", km = "12.000", valor = "89,90") }
        vm.adicionarFoto("content://foto")
        vm.salvar()
        assertTrue(vm.estado.value.concluido)
        val salvo = repositorio.observarDaMoto(moto.id).first().single()
        assertEquals(12_000, salvo.hodometroKm)
        assertEquals(8_990L, salvo.valorCentavos)
        assertEquals(listOf("foto-1.jpg"), repositorio.fotosDo(salvo.id).map { it.arquivo })
        vm.descartarFotosNaoSalvas()
        assertTrue(fotos.apagadas.isEmpty())
    }

    @Test
    fun sairSemSalvarApagaAsFotosNovas() = runTest {
        motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        val vm = novo()
        vm.adicionarFoto("content://1")
        vm.adicionarFoto("content://2")
        vm.removerFoto("foto-1.jpg")
        assertEquals(listOf("foto-1.jpg"), fotos.apagadas)
        vm.descartarFotosNaoSalvas()
        assertEquals(listOf("foto-1.jpg", "foto-2.jpg"), fotos.apagadas)
    }

    @Test
    fun feitoDoPlanoAbreComTituloEKmPreenchidos() = runTest {
        val moto = motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        hodometro.registrar(moto.id, 14_900)
        plano.semearPlanoBase(moto, kmInicial = 10_000)
        val oleo = plano.abertos().single { it.nome == "Troca de óleo" }
        val vm = novo(mapOf(RegistroFormViewModel.ARG_ITEM_ID to oleo.id))
        assertEquals(TipoRegistro.MANUTENCAO, vm.estado.value.tipo)
        assertEquals("Troca de óleo", vm.estado.value.titulo)
        assertEquals("14900", vm.estado.value.km)
        vm.salvar()
        assertEquals(14_900, plano.buscar(oleo.id)?.ultimaVezKm)
    }
}
