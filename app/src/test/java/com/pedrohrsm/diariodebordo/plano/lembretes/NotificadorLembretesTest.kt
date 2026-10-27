package com.pedrohrsm.diariodebordo.plano.lembretes

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import com.pedrohrsm.diariodebordo.MainActivity
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** A notificação de verdade (sombra do sistema): o título por urgência e o toque que abre a moto no item. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificadorLembretesTest {

    private val contexto: Context = RuntimeEnvironment.getApplication()
    private val sistema = contexto.getSystemService(NotificationManager::class.java)
    private val notificador = NotificadorDoSistema(contexto)

    private fun aviso(urgencia: Urgencia) = Aviso(
        itemId = "item-1",
        motoId = "moto-1",
        nomeMoto = "Bandida",
        nomeItem = "Troca de óleo",
        urgencia = urgencia,
    )

    private fun titulo(n: Notification) = n.extras.getString(Notification.EXTRA_TITLE)

    @Before
    fun permitir() {
        CanalLembretes.criar(contexto)
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun vencidoMostraOTituloDeVencidoEOToqueAbreAMotoNoItem() {
        assertTrue(notificador.notificar(aviso(Urgencia.VENCIDO)))
        val notificacao = shadowOf(sistema).allNotifications.single()
        assertEquals(contexto.getString(R.string.lembrete_vencido, "Troca de óleo"), titulo(notificacao))
        assertEquals(CanalLembretes.ID, notificacao.channelId)
        val intencao = shadowOf(notificacao.contentIntent).savedIntent
        assertEquals(MainActivity::class.java.name, intencao.component?.className)
        assertEquals("moto-1", intencao.getStringExtra(NotificadorLembretes.EXTRA_MOTO_ID))
        assertEquals("item-1", intencao.getStringExtra(NotificadorLembretes.EXTRA_ITEM_ID))
    }

    @Test
    fun proximoMostraOTituloDeVenceLogo() {
        assertTrue(notificador.notificar(aviso(Urgencia.PROXIMO)))
        val notificacao = shadowOf(sistema).allNotifications.single()
        assertEquals(contexto.getString(R.string.lembrete_proximo, "Troca de óleo"), titulo(notificacao))
    }

    /** Erro #26 na ponta do sistema: o aviso de vencido ocupa o lugar do de "vence logo", não fica escondido. */
    @Test
    fun vencidoDepoisDeProximoDoMesmoItemSubstituiANotificacao() {
        notificador.notificar(aviso(Urgencia.PROXIMO))
        notificador.notificar(aviso(Urgencia.VENCIDO))
        val todas = shadowOf(sistema).allNotifications
        assertEquals(1, todas.size)
        assertEquals(contexto.getString(R.string.lembrete_vencido, "Troca de óleo"), titulo(todas.single()))
    }

    @Test
    fun semPermissaoNaoNotificaEDizQueNaoNotificou() {
        shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(NotificadorLembretes.podeNotificar(contexto))
        assertFalse(notificador.notificar(aviso(Urgencia.VENCIDO)))
        assertTrue(shadowOf(sistema).allNotifications.isEmpty())
    }
}
