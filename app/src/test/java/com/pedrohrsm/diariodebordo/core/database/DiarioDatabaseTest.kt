package com.pedrohrsm.diariodebordo.core.database

import androidx.paging.PagingSource
import androidx.room.Room
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.rota.data.PontoDeRota
import com.pedrohrsm.diariodebordo.rota.data.Rota
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Abre o banco de verdade (SQLite do Robolectric) com o schema gerado pelas entidades. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DiarioDatabaseTest {

    private lateinit var banco: DiarioDatabase

    @Before
    fun abrir() {
        banco = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), DiarioDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun fechar() = banco.close()

    private fun nomes(sql: String): Set<String> = banco.openHelper.writableDatabase.query(sql).use {
        buildSet { while (it.moveToNext()) add(it.getString(0)) }
    }

    @Test
    fun abreNaVersao1ComAsTabelasDaFase1() {
        val db = banco.openHelper.writableDatabase
        db.query("PRAGMA user_version").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
        val tabelas = nomes("SELECT name FROM sqlite_master WHERE type = 'table'")
        assertTrue(
            tabelas.toString(),
            tabelas.containsAll(
                listOf("motos", "leituras_hodometro", "registros", "fotos_registro", "rotas", "pontos_rota"),
            ),
        )
    }

    /** PLANEJAMENTO §4: índice em toda coluna de relação e de ordenação das consultas principais. */
    @Test
    fun indicesDasConsultasPrincipais() {
        assertEquals(setOf("index_leituras_hodometro_motoId_km"), nomes(indices("leituras_hodometro")))
        assertEquals(
            setOf("index_registros_motoId_dataEm", "index_registros_rotaId"),
            nomes(indices("registros")),
        )
        assertEquals(setOf("index_fotos_registro_registroId"), nomes(indices("fotos_registro")))
        assertEquals(setOf("index_rotas_motoId_iniciadaEm"), nomes(indices("rotas")))
        assertEquals(setOf("index_pontos_rota_rotaId_instante"), nomes(indices("pontos_rota")))
    }

    private fun indices(tabela: String) =
        "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = '$tabela' AND name LIKE 'index_%'"

    @Test
    fun gravaELeUmaMotoDeVolta() = runTest {
        val moto = Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "Sportster 883", ano = 2012)
        banco.motoDao().inserir(moto)
        assertEquals(listOf(moto), banco.motoDao().observarAtivos().first())
    }

    @Test
    fun kmAtualEhAMaiorLeituraAtiva() = runTest {
        val dao = banco.leituraHodometroDao()
        dao.inserir(LeituraHodometro(motoId = "a", km = 1_000, lidaEm = 2))
        dao.inserir(LeituraHodometro(motoId = "a", km = 1_500, lidaEm = 1))
        dao.inserir(LeituraHodometro(motoId = "b", km = 9_000, lidaEm = 3))
        assertEquals(1_500, dao.kmAtual("a"))
        assertEquals(1_500, dao.observarKmAtual("a").first())
        assertEquals(null, dao.kmAtual("sem-leitura"))
    }

    @Test
    fun diarioPaginadoDoMaisRecenteComFiltroDeTipoESemExcluidos() = runTest {
        val dao = banco.registroDao()
        fun registro(titulo: String, tipo: TipoRegistro, dataEm: Long, motoId: String = "a") =
            Registro(motoId = motoId, tipo = tipo, titulo = titulo, dataEm = dataEm)
        dao.inserir(registro("nota antiga", TipoRegistro.NOTA, 1))
        dao.inserir(registro("abastecimento", TipoRegistro.ABASTECIMENTO, 3))
        dao.inserir(registro("nota nova", TipoRegistro.NOTA, 2))
        dao.inserir(registro("outra moto", TipoRegistro.NOTA, 4, motoId = "b"))
        val excluido = registro("excluído", TipoRegistro.NOTA, 5)
        dao.inserir(excluido)
        assertEquals(1, dao.marcarExcluido(excluido.id, quando = 6))

        assertEquals(listOf("abastecimento", "nota nova", "nota antiga"), primeiraPagina(dao.paginarDaMoto("a", null)))
        assertEquals(listOf("nota nova", "nota antiga"), primeiraPagina(dao.paginarDaMoto("a", TipoRegistro.NOTA.name)))
    }

    private suspend fun primeiraPagina(fonte: PagingSource<Int, Registro>): List<String> {
        val carga = fonte.load(PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false))
        return (carga as PagingSource.LoadResult.Page).data.map { it.titulo }
    }

    /** Decisão #17 no banco de verdade: apagar os pontos brutos de uma rota não toca nos de outra. */
    @Test
    fun pontosBrutosSaemSoDaRotaPedida() = runTest {
        val rotas = banco.rotaDao()
        val pontos = banco.pontoDeRotaDao()
        val a = Rota(motoId = "m", iniciadaEm = 1)
        val b = Rota(motoId = "m", iniciadaEm = 2)
        rotas.inserir(a)
        rotas.inserir(b)
        pontos.inserir(PontoDeRota(rotaId = a.id, latitude = -23.5, longitude = -46.6, instante = 10))
        pontos.inserir(PontoDeRota(rotaId = a.id, latitude = -23.6, longitude = -46.7, instante = 20))
        pontos.inserir(PontoDeRota(rotaId = b.id, latitude = -22.9, longitude = -43.2, instante = 30))

        assertEquals(2, pontos.apagarDaRota(a.id))
        assertTrue(pontos.daRota(a.id).isEmpty())
        assertEquals(1, pontos.daRota(b.id).size)
        assertEquals(b, rotas.emGravacao())
    }
}
