package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.lifecycle.SavedStateHandle
import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.core.preferencias.FakePreferencias
import com.pedrohrsm.diariodebordo.diario.data.FakeFotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.FakeRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.RegistroRepositorio
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.diario.ui.GuardaFotosFalsa
import com.pedrohrsm.diariodebordo.diario.ui.RegistroFormViewModel
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.FakeMotoDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoAtual
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.localizacao.FonteLocalizacaoFalsa
import com.pedrohrsm.diariodebordo.plano.data.FakeItemDoPlanoDao
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import com.pedrohrsm.diariodebordo.plano.dominio.NOME_TROCA_DE_OLEO
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Critério do item 1.8: registrar a troca de óleo em até 3 toques. Toque 1 abre a moto na garagem,
 * toque 2 é o atalho "Registrar troca de óleo" e toque 3 é Salvar, sem digitar nada.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrocaDeOleoEmTresToquesTest {

    private val relogio = Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val motos = MotoRepositorio(FakeMotoDao(), relogio)
    private val preferencias = FakePreferencias()
    private val hodometro = HodometroRepositorio(FakeLeituraHodometroDao(), relogio, AgendadorFalso())
    private val itens = FakeItemDoPlanoDao()
    private val plano = PlanoRepositorio(itens, relogio, AgendadorFalso())
    private val registros = FakeRegistroDao()
    private val repositorio =
        RegistroRepositorio(registros, FakeFotoRegistroDao(), hodometro, plano, TransacaoFalsa(), relogio)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun atalhoAbreORegistroPreenchidoESalvarCumpreOItem() = runTest(UnconfinedTestDispatcher()) {
        val moto = motos.criar(Moto(apelido = "M800", marca = "Suzuki", modelo = "Boulevard M800"))
        plano.semearPlanoBase(moto, kmInicial = 10_000)
        hodometro.registrar(moto.id, 14_800)

        // Toque 1: abrir a moto.
        val detalhe = MotoDetalheViewModel(
            SavedStateHandle(mapOf(MotoDetalheViewModel.ARG_MOTO_ID to moto.id)),
            motos,
            hodometro,
            plano,
            MotoAtual(motos, preferencias),
        )
        val coleta = launch { detalhe.estado.collect {} }
        val itemId = (detalhe.estado.value as MotoDetalheEstado.Pronto).trocaDeOleoId
        assertEquals(NOME_TROCA_DE_OLEO, itemId?.let { plano.buscar(it) }?.nome)
        coleta.cancel()

        // Toque 2: atalho abre o formulário com o item.
        val form = RegistroFormViewModel(
            SavedStateHandle(mapOf(RegistroFormViewModel.ARG_ITEM_ID to itemId)),
            repositorio,
            GuardaFotosFalsa(),
            MotoAtual(motos, preferencias),
            plano,
            hodometro,
            FonteLocalizacaoFalsa(),
            relogio,
        )
        assertEquals(NOME_TROCA_DE_OLEO, form.estado.value.titulo)
        assertEquals("14800", form.estado.value.km)

        // Toque 3: salvar.
        form.salvar()

        assertTrue(form.estado.value.concluido)
        val registro = registros.observarDaMoto(moto.id).first().single()
        assertEquals(TipoRegistro.MANUTENCAO, registro.tipo)
        val oleo = checkNotNull(plano.buscar(itemId!!))
        assertEquals(14_800, oleo.ultimaVezKm)
        assertEquals(registro.id, oleo.ultimoRegistroId)
    }
}
