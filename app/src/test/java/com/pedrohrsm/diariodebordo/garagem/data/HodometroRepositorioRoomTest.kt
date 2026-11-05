package com.pedrohrsm.diariodebordo.garagem.data

import androidx.room.Room
import com.pedrohrsm.diariodebordo.core.database.DiarioDatabase
import com.pedrohrsm.diariodebordo.core.database.TransacaoRoom
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.dominio.ExclusaoRecusadaException
import com.pedrohrsm.diariodebordo.garagem.dominio.ValidacaoExclusao
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlanoDao
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * A correção do hodômetro no SQLite de verdade (Room em memória, [TransacaoRoom]): o rollback quando a inserção
 * falha e o SQL novo do DAO (`ativasDaMoto`, `marcarExcluida`, `registrosComKm`), que o DAO falso não exercita.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HodometroRepositorioRoomTest {

    private val relogio = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private lateinit var banco: DiarioDatabase
    private val dao get() = banco.leituraHodometroDao()
    private val agendador = AgendadorFalso()

    @Before
    fun abrir() {
        banco = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), DiarioDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun fechar() = banco.close()

    private fun repositorio(leituras: LeituraHodometroDao = dao, itens: ItemDoPlanoDao = banco.itemDoPlanoDao()) =
        HodometroRepositorio(
            leituras,
            TransacaoRoom(banco),
            relogio,
            agendador,
            PlanoRepositorio(itens, TransacaoRoom(banco), relogio, AgendadorFalso()),
        )

    /** Deixa tudo passar para o DAO de verdade, menos a inserção, que falha no meio da transação. */
    private fun daoQueFalhaNaInsercao(): LeituraHodometroDao = object : LeituraHodometroDao by dao {
        override suspend fun inserir(item: LeituraHodometro) = throw IOException("disco cheio")
    }

    @Test
    fun inserirQueFalhaDesfazAExclusaoELeituraAntigaContinuaAtiva() = runTest {
        val repositorio = repositorio()
        val primeira = repositorio.registrar("moto", 1_000, lidaEm = 1_000)
        val segunda = repositorio.registrar("moto", 2_000, lidaEm = 2_000)
        val recalculos = agendador.recalculos

        val falha = runCatching { repositorio(daoQueFalhaNaInsercao()).corrigir(segunda.id, 1_500) }.exceptionOrNull()

        assertTrue(falha is IOException)
        // A marcação de excluída foi desfeita junto: as duas leituras seguem ativas, com o km de antes.
        assertEquals(listOf(primeira.id, segunda.id), dao.ativasDaMoto("moto").map { it.id })
        assertNull(dao.buscar(segunda.id)?.excluidoEm)
        assertEquals(2_000, dao.kmAtual("moto"))
        assertEquals(recalculos, agendador.recalculos)
    }

    @Test
    fun corrigirNoBancoDeVerdadeMarcaUmaLinhaExcluidaEInsereUmaNovaComAMesmaData() = runTest {
        val repositorio = repositorio()
        repositorio.registrar("moto", 1_000, lidaEm = 1_000)
        val ultima = repositorio.registrar("moto", 3_000, lidaEm = 3_000)

        val nova = repositorio.corrigir(ultima.id, 2_500)

        assertNotNull(nova.id)
        assertEquals(2_500, dao.kmAtual("moto"))
        assertEquals(listOf(1_000, 2_500), dao.ativasDaMoto("moto").map { it.km })
        assertEquals(3_000, dao.ativasDaMoto("moto").last().lidaEm)
        // A antiga não aparece mais como ativa: a consulta do km atual e a lista só enxergam a nova.
        assertNull(dao.buscar(ultima.id))
        assertEquals(listOf(2_500, 1_000), repositorio.observarDaMoto("moto").first().map { it.km })
        assertEquals(listOf(KmAtual("moto", 2_500)), repositorio.observarKmAtuais().first())
    }

    @Test
    fun marcarExcluidaDevolveUmaLinhaNaPrimeiraVezEZeroNaSegunda() = runTest {
        val leitura = repositorio().registrar("moto", 1_000, lidaEm = 1_000)
        assertEquals(1, dao.marcarExcluida(leitura.id, quando = 5))
        assertEquals(0, dao.marcarExcluida(leitura.id, quando = 6))
        assertEquals(0, dao.marcarExcluida("inexistente", quando = 7))
    }

    @Test
    fun registrosComKmContaSoOsAtivosDaMotoComKm() = runTest {
        fun registro(motoId: String, km: Int?, excluidoEm: Long? = null) = Registro(
            motoId = motoId,
            tipo = TipoRegistro.NOTA,
            titulo = "nota",
            dataEm = 1,
            hodometroKm = km,
            excluidoEm = excluidoEm,
        )
        listOf(
            registro("moto", km = 1_000),
            registro("moto", km = 2_000),
            registro("moto", km = null),
            registro("moto", km = 3_000, excluidoEm = 9),
            registro("outra", km = 4_000),
        ).forEach { banco.registroDao().inserir(it) }
        assertEquals(2, dao.registrosComKm("moto"))
        assertEquals(0, dao.registrosComKm("sem-registros"))
    }

    @Test
    fun apagarAUnicaLeituraDeMotoComRegistroDeKmEhRecusadoNoBancoDeVerdade() = runTest {
        val repositorio = repositorio()
        val unica = repositorio.registrar("moto", 1_000, lidaEm = 1_000)
        banco.registroDao().inserir(
            Registro(motoId = "moto", tipo = TipoRegistro.NOTA, titulo = "nota", dataEm = 1, hodometroKm = 1_000),
        )

        val recusa = runCatching { repositorio.apagar(unica.id) }.exceptionOrNull()

        assertEquals(ValidacaoExclusao.UnicaComRegistrosDeKm, (recusa as ExclusaoRecusadaException).motivo)
        assertEquals(1_000, dao.kmAtual("moto"))
        // Com uma segunda leitura, a primeira pode sair mesmo havendo registro com km.
        repositorio.registrar("moto", 1_200, lidaEm = 2_000)
        repositorio.apagar(unica.id)
        assertEquals(1_200, dao.kmAtual("moto"))
    }

    /** Óleo feito aos 3.000 km: a leitura que a correção leva junto (#69). */
    private suspend fun oleoFeitoAos3000() = ItemDoPlano(
        id = "oleo",
        motoId = "moto",
        nome = "Óleo",
        intervaloKm = 3_000,
        ultimaVezKm = 3_000,
        ultimaVezEm = 3_000,
    ).also { banco.itemDoPlanoDao().inserir(it) }

    @Test
    fun corrigirNoBancoDeVerdadeLevaJuntoOItemDoPlano() = runTest {
        val repositorio = repositorio()
        repositorio.registrar("moto", 1_000, lidaEm = 1_000)
        val ultima = repositorio.registrar("moto", 3_000, lidaEm = 3_000)
        oleoFeitoAos3000()

        repositorio.corrigir(ultima.id, 2_500)

        assertEquals(2_500, banco.itemDoPlanoDao().buscar("oleo")?.ultimaVezKm)
    }

    @Test
    fun falhaAoGravarOPlanoDesfazACorrecaoDaLeitura() = runTest {
        val repositorio = repositorio()
        val primeira = repositorio.registrar("moto", 1_000, lidaEm = 1_000)
        val ultima = repositorio.registrar("moto", 3_000, lidaEm = 3_000)
        oleoFeitoAos3000()
        val recalculos = agendador.recalculos
        val quebrado = object : ItemDoPlanoDao by banco.itemDoPlanoDao() {
            override suspend fun atualizar(item: ItemDoPlano): Int = throw IOException("disco cheio")
        }

        val falha = runCatching { repositorio(itens = quebrado).corrigir(ultima.id, 2_500) }.exceptionOrNull()

        assertTrue(falha is IOException)
        // A leitura antiga segue ativa e a nova não entrou; os lembretes não foram pedidos.
        assertEquals(listOf(primeira.id, ultima.id), dao.ativasDaMoto("moto").map { it.id })
        assertEquals(3_000, dao.kmAtual("moto"))
        assertEquals(recalculos, agendador.recalculos)
    }

    @Test
    fun falhaNoSegundoItemDesfazOPrimeiroItemDoPlano() = runTest {
        val repositorio = repositorio()
        repositorio.registrar("moto", 1_000, lidaEm = 1_000)
        val ultima = repositorio.registrar("moto", 3_000, lidaEm = 3_000)
        val oleo = oleoFeitoAos3000()
        val corrente = oleo.copy(id = "relacao", nome = "Relação").also { banco.itemDoPlanoDao().inserir(it) }
        // O óleo grava; a relação falha. O óleo já gravado volta junto com a leitura.
        val quebrado = object : ItemDoPlanoDao by banco.itemDoPlanoDao() {
            override suspend fun atualizar(item: ItemDoPlano): Int =
                if (item.id == corrente.id) throw IOException("disco cheio") else banco.itemDoPlanoDao().atualizar(item)
        }

        runCatching { repositorio(itens = quebrado).corrigir(ultima.id, 2_500) }

        assertEquals(oleo, banco.itemDoPlanoDao().buscar("oleo"))
    }
}
