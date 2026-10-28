package com.pedrohrsm.diariodebordo.plano.data

import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.diario.data.FakeFotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.FakeRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.RegistroRepositorio
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.FakeLeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio
import com.pedrohrsm.diariodebordo.plano.dominio.CHAVE_TROCA_DE_OLEO
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AtalhoTrocaDeOleoTest {
    private val agora = Instant.parse("2026-10-27T15:00:00Z")
    private val relogio = Clock.fixed(agora, ZoneId.of("America/Sao_Paulo"))
    private val transacao = TransacaoFalsa()
    private val itensDao = FakeItemDoPlanoDao()
    private val registrosDao = FakeRegistroDao()
    private val hodometro = HodometroRepositorio(FakeLeituraHodometroDao(), transacao, relogio, AgendadorFalso())
    private val atalho = AtalhoTrocaDeOleo(
        PlanoRepositorio(itensDao, transacao, relogio, AgendadorFalso()),
        RegistroRepositorio(registrosDao, FakeFotoRegistroDao(), hodometro, transacao, relogio),
        hodometro,
        transacao,
        relogio,
    )

    private val oleo = ItemDoPlano(
        id = "oleo",
        motoId = "m",
        chave = CHAVE_TROCA_DE_OLEO,
        nome = "Óleo 10W40",
        intervaloKm = 5_000,
        ultimaVezKm = 10_000,
        origem = OrigemItem.PLANO_BASE,
    )

    @Test
    fun registrarMarcaOItemFeitoEDeixaAManutencaoNoDiario() = runTest {
        itensDao.linhas.value = listOf(oleo, oleo.copy(id = "outro", chave = null, nome = "Troca de óleo"))
        hodometro.registrar("m", 14_870)

        val registro = atalho.registrar("m")

        val feito = itensDao.linhas.value.single { it.id == "oleo" }
        assertEquals(14_870, feito.ultimaVezKm)
        assertEquals(agora.toEpochMilli(), feito.feitoEm)
        assertEquals(
            "o item da pessoa com o mesmo nome não é tocado",
            oleo.copy(id = "outro", chave = null, nome = "Troca de óleo"),
            itensDao.linhas.value.single {
                it.id ==
                    "outro"
            },
        )
        assertEquals(TipoRegistro.MANUTENCAO, registro.tipo)
        assertEquals("Óleo 10W40", registro.titulo)
        assertEquals(14_870, registro.hodometroKm)
        assertEquals(listOf(registro), registrosDao.linhas.value)
    }

    @Test
    fun motoSemItemDeOleoNaoGravaNada() = runTest {
        itensDao.linhas.value = listOf(oleo.copy(excluidoEm = 1))
        try {
            atalho.registrar("m")
            fail("deveria recusar")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("sem item de troca de óleo"))
        }
        assertTrue(registrosDao.linhas.value.isEmpty())
    }
}
