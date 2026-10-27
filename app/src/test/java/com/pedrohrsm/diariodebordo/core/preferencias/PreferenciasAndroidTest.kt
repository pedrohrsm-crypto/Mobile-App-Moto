package com.pedrohrsm.diariodebordo.core.preferencias

import android.content.Context
import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PreferenciasAndroidTest {
    private val contexto: Context = RuntimeEnvironment.getApplication()

    @Test
    fun ajusteSalvoVoltaNaLeituraImediataENoFluxo() = runTest {
        val escolhido = AjusteTema(TemaDiario.ESTRADA, modoOficina = true)
        PreferenciasAndroid(contexto).salvar(escolhido)
        // Outra instância, como na próxima abertura do app: a leitura imediata já traz o tema escolhido.
        val reaberto = PreferenciasAndroid(contexto)
        assertEquals(escolhido, reaberto.ajusteSalvo())
        assertEquals(escolhido, reaberto.ajuste.first())
    }

    @Test
    fun semNadaSalvoOuComTemaDesconhecidoValeOTemaDePartida() {
        val prefs = contexto.getSharedPreferences("preferencias", Context.MODE_PRIVATE)
        assertEquals(AjusteTema(), PreferenciasAndroid(contexto).ajusteSalvo())
        prefs.edit().putString("tema", "NEON").commit()
        assertEquals(AjusteTema(), PreferenciasAndroid(contexto).ajusteSalvo())
    }
}
