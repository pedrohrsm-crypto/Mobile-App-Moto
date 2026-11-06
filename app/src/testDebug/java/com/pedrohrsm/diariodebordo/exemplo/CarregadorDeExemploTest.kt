package com.pedrohrsm.diariodebordo.exemplo

import androidx.room.Room
import com.pedrohrsm.diariodebordo.core.database.DiarioDatabase
import com.pedrohrsm.diariodebordo.core.database.TransacaoRoom
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** O botão do debug grava no Room de verdade e, apertado de novo, não duplica nada (#77). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CarregadorDeExemploTest {

    private val banco = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), DiarioDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val carregador = CarregadorDeExemplo(
        banco.backupDao(),
        TransacaoRoom(banco),
        Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneId.of("America/Sao_Paulo")),
    )

    @After
    fun fechar() = banco.close()

    @Test
    fun carregarDuasVezesGravaAsMesmasLinhasSemDuplicar() = runTest {
        val dados = carregador.carregar()
        carregador.carregar()
        val dao = banco.backupDao()
        assertEquals(dados.motos.size, dao.motos().size)
        assertEquals(dados.leituras.size, dao.leituras().size)
        assertEquals(dados.registros.size, dao.registros().size)
        assertEquals(dados.itensDoPlano.size, dao.itensDoPlano().size)
        assertEquals(dados.rotas.size, dao.rotas().size)
    }
}
