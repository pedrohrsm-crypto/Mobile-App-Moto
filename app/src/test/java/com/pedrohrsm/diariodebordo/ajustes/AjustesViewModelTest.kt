package com.pedrohrsm.diariodebordo.ajustes

import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import com.pedrohrsm.diariodebordo.core.preferencias.FakePreferencias
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AjustesViewModelTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun comecaNoTemaOficinaSemModoOficinaEGuardaAsEscolhas() = runTest(UnconfinedTestDispatcher()) {
        val preferencias = FakePreferencias()
        val vm = AjustesViewModel(preferencias)
        val coleta = launch { vm.estado.collect {} }
        assertEquals(AjustesEstado(TemaDiario.OFICINA, modoOficina = false), vm.estado.value)

        vm.escolherTema(TemaDiario.CROMO)
        vm.alternarModoOficina(true)

        assertEquals(AjustesEstado(TemaDiario.CROMO, modoOficina = true), vm.estado.value)
        assertEquals(TemaDiario.CROMO, preferencias.tema.first())
        assertEquals(true, preferencias.modoOficina.first())
        coleta.cancel()
    }
}
