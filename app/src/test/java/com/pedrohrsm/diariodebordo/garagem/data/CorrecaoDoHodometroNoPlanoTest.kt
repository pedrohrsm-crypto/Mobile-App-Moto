package com.pedrohrsm.diariodebordo.garagem.data

import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.plano.data.FakeItemDoPlanoDao
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import com.pedrohrsm.diariodebordo.plano.dominio.situacao
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A correção do hodômetro leva junto o plano (#69): o item feito no km da leitura corrigida passa ao km novo; o item
 * feito noutro km não muda; apagar uma leitura não mexe no plano. O caminho da issue: 50.000 lançado em vez de 5.000.
 */
class CorrecaoDoHodometroNoPlanoTest {

    private val instante = Instant.parse("2026-10-01T12:00:00Z")
    private val relogio = Clock.fixed(instante, ZoneId.of("America/Sao_Paulo"))
    private val transacao = TransacaoFalsa()
    private val agendador = AgendadorFalso()
    private val itens = FakeItemDoPlanoDao()
    private val plano = PlanoRepositorio(itens, transacao, relogio, agendador)
    private val hodometro = HodometroRepositorio(FakeLeituraHodometroDao(), transacao, relogio, agendador, plano)

    private fun item(id: String, ultimaVezKm: Int) = ItemDoPlano(
        id = id,
        motoId = "moto",
        nome = id,
        intervaloKm = 3_000,
        ultimaVezKm = ultimaVezKm,
        ultimaVezEm = instante.toEpochMilli(),
        avisadoComo = Urgencia.PROXIMO,
    )

    private fun linha(id: String) = itens.linhas.value.single { it.id == id }

    /** Leituras de 4.000 e 50.000 (a errada); o óleo foi marcado feito nos 50.000 e o filtro, nos 4.000. */
    private suspend fun lancadoErrado(): LeituraHodometro {
        hodometro.registrar("moto", 4_000, lidaEm = 1_000)
        val errada = hodometro.registrar("moto", 50_000, lidaEm = 2_000)
        itens.inserir(item("oleo", ultimaVezKm = 50_000))
        itens.inserir(item("filtro", ultimaVezKm = 4_000))
        return errada
    }

    @Test
    fun corrigirLeituraLevaJuntoOItemFeitoNoKmAntigo() = runTest {
        val errada = lancadoErrado()

        hodometro.corrigir(errada.id, 5_000)

        assertEquals(5_000, linha("oleo").ultimaVezKm)
    }

    @Test
    fun corrigirLeituraNaoMexeNoItemFeitoEmOutroKm() = runTest {
        val errada = lancadoErrado()
        val antes = linha("filtro")

        hodometro.corrigir(errada.id, 5_000)

        assertEquals(antes, linha("filtro"))
    }

    @Test
    fun corrigirLeituraRecalculaAUrgenciaEOsLembretesDoItem() = runTest {
        val errada = lancadoErrado()
        // Com o km errado, o óleo está em dia até 53.000; o certo é vencer aos 8.000.
        assertEquals(Urgencia.EM_DIA, linha("oleo").situacao(50_000, relogio)?.urgencia)
        val recalculos = agendador.recalculos

        hodometro.corrigir(errada.id, 5_000)
        hodometro.registrar("moto", 8_500)

        assertEquals(Urgencia.VENCIDO, linha("oleo").situacao(hodometro.kmAtual("moto"), relogio)?.urgencia)
        // O aviso antigo deixa de valer: o lembrete de vencido sai na próxima verificação.
        assertNull(linha("oleo").avisadoComo)
        assertEquals(recalculos + 2, agendador.recalculos)
    }

    @Test
    fun apagarLeituraNaoMexeNoPlano() = runTest {
        val errada = lancadoErrado()
        val antes = itens.linhas.value

        hodometro.apagar(errada.id)

        assertEquals(antes, itens.linhas.value)
    }
}
