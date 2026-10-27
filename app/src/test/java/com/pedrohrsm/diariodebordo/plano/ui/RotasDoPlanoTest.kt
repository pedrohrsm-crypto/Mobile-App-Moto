package com.pedrohrsm.diariodebordo.plano.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import com.pedrohrsm.diariodebordo.navegacao.Abertura
import com.pedrohrsm.diariodebordo.navegacao.Destino
import com.pedrohrsm.diariodebordo.navegacao.Rotas
import com.pedrohrsm.diariodebordo.navegacao.comoAbertura
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import com.pedrohrsm.diariodebordo.plano.lembretes.Aviso
import com.pedrohrsm.diariodebordo.plano.lembretes.CanalLembretes
import com.pedrohrsm.diariodebordo.plano.lembretes.NotificadorDoSistema
import com.pedrohrsm.diariodebordo.plano.lembretes.NotificadorLembretes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** As rotas do plano: a que a notificação abre e a que o formulário edita. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RotasDoPlanoTest {

    @Test
    fun planoDaMotoCarregaOItemDestacadoCodificado() {
        assertEquals("moto/m1/plano?destaque=i1", Rotas.planoDaMoto("m1", "i1"))
        assertEquals("moto/m1/plano?destaque=a%20b%26c", Rotas.planoDaMoto("m1", "a b&c"))
        assertEquals("moto/m1/plano", Rotas.planoDaMoto("m1", null))
    }

    @Test
    fun planoEhUmaAbaEAsRotasDeItemSaoDeTelaCheia() {
        assertTrue(Destino.entries.any { it.rota == Rotas.PLANO })
        assertTrue(Destino.entries.none { it.rota == Rotas.PLANO_DA_MOTO || it.rota == Rotas.ITEM })
        assertEquals("plano/item/abc", Rotas.item("abc"))
    }

    @Test
    fun oToqueNaNotificacaoViraAAberturaDaMotoNoItem() {
        val contexto = RuntimeEnvironment.getApplication()
        val sistema = contexto.getSystemService(NotificationManager::class.java)
        CanalLembretes.criar(contexto)
        shadowOf(contexto).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val aviso = Aviso("item-9", "moto-3", "Bandida", "Troca de óleo", Urgencia.VENCIDO)
        assertTrue(NotificadorDoSistema(contexto).notificar(aviso))
        val toque = shadowOf(shadowOf(sistema).allNotifications.single().contentIntent).savedIntent
        assertEquals(Abertura("moto-3", "item-9"), toque.comoAbertura())
    }

    @Test
    fun intencaoSemOsExtrasDoLembreteNaoAbreNada() {
        assertNull(Intent().comoAbertura())
        assertEquals(Abertura("m1", null), Intent().putExtra(NotificadorLembretes.EXTRA_MOTO_ID, "m1").comoAbertura())
        assertNull(Intent().putExtra(NotificadorLembretes.EXTRA_ITEM_ID, "i1").comoAbertura())
    }
}
