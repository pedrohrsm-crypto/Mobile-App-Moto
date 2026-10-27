package com.pedrohrsm.diariodebordo.plano.lembretes

import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.FakeMotoDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.plano.data.FakeItemDoPlanoDao
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A verificação inteira, do banco ao aviso: é o que o WorkManager roda com o app fechado. */
class VerificadorLembretesTest {

    private val agora = Instant.parse("2026-10-01T12:00:00Z")
    private val relogio = Clock.fixed(agora, ZoneId.of("America/Sao_Paulo"))
    private val planoDao = FakeItemDoPlanoDao()
    private val motos = MotoRepositorio(FakeMotoDao(), relogio)
    private val hodometro =
        HodometroRepositorio(FakeLeituraHodometroDao(), TransacaoFalsa(), relogio, AgendadorFalso())
    private val plano = PlanoRepositorio(planoDao, TransacaoFalsa(), relogio, AgendadorFalso())
    private val notificador = NotificadorFalso()
    private val verificador = VerificadorLembretes(motos, hodometro, plano, notificador, relogio)

    /** Óleo a cada 5.000 km, feito aos 10.000: PRÓXIMO a partir dos 14.500 e VENCIDO a partir dos 15.000. */
    private suspend fun motoComOleo(kmAtual: Int): ItemDoPlano {
        val moto = motos.criar(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        hodometro.registrar(moto.id, kmAtual)
        return plano.salvar(
            ItemDoPlano(motoId = moto.id, nome = "Troca de óleo", intervaloKm = 5_000, ultimaVezKm = 10_000),
            kmAtual = kmAtual,
        )
    }

    private suspend fun avisadoComo(item: ItemDoPlano) = plano.buscar(item.id)?.avisadoComo

    @Test
    fun proximoAvisadoDepoisVencidoAvisaDeNovoEDepoisCala() = runTest {
        val item = motoComOleo(kmAtual = 14_600)
        assertEquals(1, verificador.verificar())
        assertEquals(listOf(Urgencia.PROXIMO), notificador.avisos.map { it.urgencia })
        assertEquals(Urgencia.PROXIMO, avisadoComo(item))
        // Mesma urgência: não repete.
        assertEquals(0, verificador.verificar())

        hodometro.registrar(item.motoId, 15_100)
        assertEquals(1, verificador.verificar())
        assertEquals(listOf(Urgencia.PROXIMO, Urgencia.VENCIDO), notificador.avisos.map { it.urgencia })
        assertEquals(Urgencia.VENCIDO, avisadoComo(item))
        assertEquals("Troca de óleo", notificador.avisos.last().nomeItem)
        assertEquals(item.motoId, notificador.avisos.last().motoId)
        assertEquals(item.id, notificador.avisos.last().itemId)
        assertEquals(0, verificador.verificar())
    }

    @Test
    fun itemQueSaltaDireitoParaVencidoAvisaUmaVezSo() = runTest {
        val item = motoComOleo(kmAtual = 15_500)
        assertEquals(1, verificador.verificar())
        assertEquals(0, verificador.verificar())
        assertEquals(listOf(Urgencia.VENCIDO), notificador.avisos.map { it.urgencia })
        assertEquals(Urgencia.VENCIDO, avisadoComo(item))
    }

    @Test
    fun aparelhoQueNaoDeixaNotificarNaoMarcaEAvisaNaProximaVerificacao() = runTest {
        val item = motoComOleo(kmAtual = 15_500)
        notificador.aceita = false
        assertEquals(0, verificador.verificar())
        assertEquals(null, avisadoComo(item))
        notificador.aceita = true
        assertEquals(1, verificador.verificar())
        assertEquals(Urgencia.VENCIDO, avisadoComo(item))
    }

    @Test
    fun feitoDeNovoRecomecaOsAvisos() = runTest {
        val item = motoComOleo(kmAtual = 15_500)
        verificador.verificar()
        plano.marcarFeito(item.id, kmAtual = 15_500)
        assertEquals(0, verificador.verificar())
        hodometro.registrar(item.motoId, 20_600)
        assertEquals(1, verificador.verificar())
        assertEquals(listOf(Urgencia.VENCIDO, Urgencia.VENCIDO), notificador.avisos.map { it.urgencia })
    }

    @Test
    fun itemExcluidoNoMeioDoEnvioNaoContaComoAvisoMarcado() = runTest {
        val item = motoComOleo(kmAtual = 15_500)
        notificador.aoNotificar = { runBlocking { plano.excluir(item.id) } }
        assertEquals(0, verificador.verificar())
        assertEquals(null, plano.buscar(item.id))
    }

    @Test
    fun bancoIndisponivelSobeComoErroENaoViraSemAvisos() = runTest {
        motoComOleo(kmAtual = 15_500)
        planoDao.falha = IllegalStateException("banco fechado")
        val resultado = runCatching { verificador.verificar() }
        assertTrue(resultado.exceptionOrNull() is IllegalStateException)
        assertTrue(notificador.avisos.isEmpty())
    }
}
