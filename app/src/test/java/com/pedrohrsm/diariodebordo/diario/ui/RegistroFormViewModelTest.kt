package com.pedrohrsm.diariodebordo.diario.ui

import androidx.lifecycle.SavedStateHandle
import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
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
import com.pedrohrsm.diariodebordo.garagem.data.MotoDao
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.localizacao.FonteLocalizacaoFalsa
import com.pedrohrsm.diariodebordo.localizacao.LocalIndisponivelException
import com.pedrohrsm.diariodebordo.localizacao.Ponto
import com.pedrohrsm.diariodebordo.localizacao.SemPermissaoDeLocalizacaoException
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    private val motoAtual = MotoAtual(motos)
    private val registros = FakeRegistroDao()
    private val hodometro = HodometroRepositorio(FakeLeituraHodometroDao(), TransacaoFalsa(), relogio, AgendadorFalso())
    private val repositorio =
        RegistroRepositorio(registros, FakeFotoRegistroDao(), hodometro, TransacaoFalsa(), relogio)
    private val fotos = GuardaFotosFalsa()
    private val gps = FonteLocalizacaoFalsa()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun novo(args: Map<String, Any?> = emptyMap()) =
        RegistroFormViewModel(SavedStateHandle(args), repositorio, fotos, motoAtual, gps, relogio)

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
        val salvo = registros.daMoto(moto.id).single()
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
    fun semMotoNaoSalvaEAvisa() = runTest {
        val vm = novo()
        assertEquals(ErroRegistroForm.SemMoto, vm.estado.value.erro)
        vm.alterar { it.copy(titulo = "Nota") }
        vm.salvar()
        assertTrue(registros.linhas.value.isEmpty())
    }

    /** A tela preenche o título padrão por alterar() logo depois da carga: o aviso de carga não pode sumir. */
    @Test
    fun avisoDeCargaSobreviveAoTituloPadraoEAEdicao() = runTest {
        val semMoto = novo()
        semMoto.alterar { it.copy(titulo = "Manutenção") }
        semMoto.adicionarFoto("content://foto")
        assertEquals(ErroRegistroForm.SemMoto, semMoto.estado.value.erro)

        val inexistente = novo(mapOf(RegistroFormViewModel.ARG_REGISTRO_ID to "nao-existe"))
        inexistente.alterar { it.copy(titulo = "Manutenção") }
        assertEquals(ErroRegistroForm.NaoEncontrado, inexistente.estado.value.erro)
    }

    @Test
    fun falhaAoCarregarSobreviveAoTituloPadrao() = runTest {
        val quebrado = object : MotoDao by FakeMotoDao() {
            override fun observarAtivos(): Flow<List<Moto>> = flow { throw IllegalStateException("disco cheio") }
        }
        val vm = RegistroFormViewModel(
            SavedStateHandle(),
            repositorio,
            fotos,
            MotoAtual(MotoRepositorio(quebrado, relogio)),
            gps,
            relogio,
        )
        vm.alterar { it.copy(titulo = "Manutenção") }
        assertTrue(vm.estado.value.erro.toString(), vm.estado.value.erro?.toString().orEmpty().contains("disco cheio"))
    }

    @Test
    fun kmDoRegistroAvancaOHodometro() = runTest {
        val moto = motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        hodometro.registrar(moto.id, 14_900)
        val vm = novo()
        vm.alterar { it.copy(titulo = "Abastecimento", km = "15.200") }
        vm.salvar()
        assertEquals(15_200, hodometro.kmAtual(moto.id))
    }

    /** 1.6: o local entra no registro salvo, e editar o registro mostra o mesmo local. */
    @Test
    fun localAtualEntraNoRegistroSalvo() = runTest {
        motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        gps.resposta = Ponto(-23.5505, -46.6333, instante = 0)
        val vm = novo()
        vm.alterar { it.copy(titulo = "Parada no mirante") }
        vm.usarLocalAtual()
        vm.salvar()
        val salvo = registros.linhas.value.single()
        assertEquals(-23.5505, salvo.latitude!!, 0.0)
        assertEquals(-46.6333, salvo.longitude!!, 0.0)

        val editando = novo(mapOf(RegistroFormViewModel.ARG_REGISTRO_ID to salvo.id))
        assertEquals(-23.5505, editando.estado.value.latitude!!, 0.0)
    }

    /** 1.6: negar a permissão avisa e não impede salvar; o registro fica sem local. */
    @Test
    fun negarAPermissaoNaoQuebraORegistro() = runTest {
        motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        val vm = novo()
        vm.alterar { it.copy(titulo = "Nota") }
        vm.localNegado()
        assertEquals(ErroRegistroForm.SemPermissaoLocal, vm.estado.value.erro)
        vm.salvar()
        assertTrue(vm.estado.value.concluido)
        val salvo = registros.linhas.value.single()
        assertEquals(null, salvo.latitude)
        assertEquals(null, salvo.longitude)
    }

    /** A permissão pode ser retirada entre o toque e a leitura: vira o mesmo aviso, sem travar o formulário. */
    @Test
    fun permissaoRetiradaNoMeioViraAviso() = runTest {
        motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        gps.falha = SemPermissaoDeLocalizacaoException()
        val vm = novo()
        vm.usarLocalAtual()
        assertEquals(ErroRegistroForm.SemPermissaoLocal, vm.estado.value.erro)
        assertEquals(false, vm.estado.value.buscandoLocal)
    }

    @Test
    fun gpsSemRespostaAvisaESegueSemLocal() = runTest {
        motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        gps.falha = LocalIndisponivelException()
        val vm = novo()
        vm.usarLocalAtual()
        assertEquals(ErroRegistroForm.LocalIndisponivel, vm.estado.value.erro)
        assertEquals(null, vm.estado.value.latitude)
        assertEquals(false, vm.estado.value.buscandoLocal)
    }

    @Test
    fun removerLocalSalvaSemEle() = runTest {
        motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        gps.resposta = Ponto(-23.5505, -46.6333, instante = 0)
        val vm = novo()
        vm.alterar { it.copy(titulo = "Nota") }
        vm.usarLocalAtual()
        vm.removerLocal()
        vm.salvar()
        assertEquals(null, registros.linhas.value.single().latitude)
    }
}
