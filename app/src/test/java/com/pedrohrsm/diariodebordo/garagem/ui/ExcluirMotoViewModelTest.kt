package com.pedrohrsm.diariodebordo.garagem.ui

import androidx.lifecycle.SavedStateHandle
import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.diario.ui.GuardaFotosFalsa
import com.pedrohrsm.diariodebordo.garagem.data.ExclusaoDeMoto
import com.pedrohrsm.diariodebordo.garagem.data.ExclusaoDeMotoDao
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * O diálogo de exclusão depois do commit (#72): falha em apagar uma foto não vira "nada foi apagado"; vira aviso, e a
 * tela volta para a garagem quando a pessoa fecha o aviso.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExcluirMotoViewModelTest {

    private val relogio = Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val fotos = GuardaFotosFalsa()
    private val agendador = AgendadorFalso()
    private val dao = DaoDeUmaMoto()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun aberto(): ExcluirMotoViewModel {
        val exclusao = ExclusaoDeMoto(dao, TransacaoFalsa(), fotos, agendador, relogio)
        return ExcluirMotoViewModel(SavedStateHandle(mapOf(MotoDetalheViewModel.ARG_MOTO_ID to "a")), exclusao).also {
            it.abrir("Bandida")
            it.alterarTexto("Bandida")
        }
    }

    @Test
    fun falhaAoApagarFotoDepoisDoCommitAvisaEVoltaParaAGaragem() {
        fotos.falhaAoApagar = { SecurityException("sem permissão") }
        val vm = aberto()

        vm.excluir()

        assertEquals(1, dao.motosMarcadas)
        assertNull(vm.dialogo.value)
        assertEquals(AvisoExclusao(fotosQueFicaram = 2, lembretesRecalculados = true), vm.aviso.value)
        // Só volta para a garagem depois que a pessoa leu o aviso.
        assertFalse(vm.excluida.value)
        vm.fecharAviso()
        assertNull(vm.aviso.value)
        assertTrue(vm.excluida.value)
    }

    @Test
    fun semFalhaDepoisDoCommitVoltaDiretoParaAGaragem() {
        val vm = aberto()

        vm.excluir()

        assertNull(vm.aviso.value)
        assertTrue(vm.excluida.value)
    }

    /** Uma moto com 2 fotos e nada mais; conta as marcações para conferir que o commit aconteceu. */
    private class DaoDeUmaMoto : ExclusaoDeMotoDao {
        var motosMarcadas = 0
        override suspend fun contarRegistros(motoId: String) = 0
        override suspend fun arquivosDasFotos(motoId: String) = listOf("a-1.jpg", "a-2.jpg")
        override suspend fun contarItensDoPlano(motoId: String) = 0
        override suspend fun contarRotas(motoId: String) = 0
        override suspend fun contarRotasEmGravacao(motoId: String) = 0
        override suspend fun contarLeituras(motoId: String) = 0
        override suspend fun marcarFotos(motoId: String, quando: Long) = 2
        override suspend fun marcarRegistros(motoId: String, quando: Long) = 0
        override suspend fun marcarItensDoPlano(motoId: String, quando: Long) = 0
        override suspend fun marcarRotas(motoId: String, quando: Long) = 0
        override suspend fun marcarLeituras(motoId: String, quando: Long) = 0
        override suspend fun marcarMoto(motoId: String, quando: Long): Int {
            motosMarcadas++
            return 1
        }
    }
}
