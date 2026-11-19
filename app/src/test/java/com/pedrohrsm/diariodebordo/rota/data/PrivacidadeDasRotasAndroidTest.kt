package com.pedrohrsm.diariodebordo.rota.data

import android.content.Context
import com.pedrohrsm.diariodebordo.rota.dominio.CorteDasPontas
import com.pedrohrsm.diariodebordo.rota.dominio.ZonaDePrivacidade
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** As zonas e o corte nas preferências do aparelho (#78), com o SharedPreferences do Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrivacidadeDasRotasAndroidTest {
    private val contexto: Context = RuntimeEnvironment.getApplication()
    private val casa = ZonaDePrivacidade(id = "casa", nome = "Casa", latitude = -27.5, longitude = -48.5, raioM = 500)

    @Test
    fun semNadaGravadoOCortePadraoEUmKmESemZonas() = runTest {
        val protecao = PrivacidadeDasRotasAndroid(contexto).atual()
        assertEquals(CorteDasPontas.KM1, protecao.corte)
        assertEquals(emptyList<ZonaDePrivacidade>(), protecao.zonas)
    }

    @Test
    fun corteEZonasVoltamNoutraInstanciaENoFluxo() = runTest {
        val privacidade = PrivacidadeDasRotasAndroid(contexto)
        privacidade.salvarCorte(CorteDasPontas.KM5)
        privacidade.salvarZona(casa)
        privacidade.salvarZona(casa.copy(raioM = 1_000))
        val reaberta = PrivacidadeDasRotasAndroid(contexto)
        assertEquals(CorteDasPontas.KM5, reaberta.atual().corte)
        assertEquals(listOf(casa.copy(raioM = 1_000)), reaberta.protecao.first().zonas)
        reaberta.excluirZona("casa")
        assertEquals(emptyList<ZonaDePrivacidade>(), reaberta.atual().zonas)
    }

    @Test(expected = IllegalStateException::class)
    fun excluirZonaQueNaoExisteLanca() = runTest {
        PrivacidadeDasRotasAndroid(contexto).excluirZona("nenhuma")
    }

    @Test
    fun segredoESorteadoUmaVezEGuardado() = runTest {
        val primeiro = PrivacidadeDasRotasAndroid(contexto).segredo()
        assertEquals(32, primeiro.size)
        assertArrayEquals(primeiro, PrivacidadeDasRotasAndroid(contexto).segredo())
    }

    /** Zonas ilegíveis nunca viram "sem zonas" (exportaria sem proteção), e o erro não leva o dado gravado. */
    @Test
    fun zonasIlegiveisLancamSemOConteudoNaMensagem() = runTest {
        contexto.getSharedPreferences("privacidade_rotas", Context.MODE_PRIVATE).edit()
            .putString("zonas", """[{"id":"x","nome":"Casa","latitude":-27.123456,""").commit()
        try {
            PrivacidadeDasRotasAndroid(contexto).atual()
            fail("leu zonas corrompidas como se fossem válidas")
        } catch (e: ZonasIlegiveisException) {
            // A cadeia inteira, como chegaria a um relatório de falha (a corrotina pode encadear uma cópia).
            assertTrue("27.12" !in e.stackTraceToString())
        }
    }
}
