package com.pedrohrsm.diariodebordo.core.database

import androidx.paging.PagingSource
import androidx.room.Room
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.TipoOcorrencia
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.OrigemItem
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
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
                listOf(
                    "motos",
                    "leituras_hodometro",
                    "registros",
                    "fotos_registro",
                    "itens_plano",
                    "rotas",
                    "pontos_rota",
                ),
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
        assertEquals(
            setOf("index_itens_plano_motoId", "index_itens_plano_registroOrigemId"),
            nomes(indices("itens_plano")),
        )
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

    @Test
    fun planoDaMotoMostraSoOsAbertosEGuardaAUrgenciaAvisada() = runTest {
        val dao = banco.itemDoPlanoDao()
        fun item(nome: String, motoId: String = "a", origem: OrigemItem = OrigemItem.USUARIO) =
            ItemDoPlano(motoId = motoId, nome = nome, intervaloKm = 5_000, origem = origem)
        val oleo = item("Óleo")
        val resolvido = item("Resolvido")
        val excluido = item("Excluído")
        listOf(oleo, item("Filtro"), item("De outra moto", motoId = "b"), resolvido, excluido).forEach {
            dao.inserir(it)
        }
        assertEquals(1, dao.atualizar(resolvido.copy(resolvidoEm = 5)))
        assertEquals(1, dao.marcarExcluido(excluido.id, quando = 6))
        assertEquals(listOf("Filtro", "Óleo"), dao.observarDaMoto("a").first().map { it.nome })

        assertEquals(1, dao.marcarAvisado(oleo.id, Urgencia.PROXIMO, quando = 7))
        assertEquals(Urgencia.PROXIMO, dao.buscar(oleo.id)?.avisadoComo)
        assertEquals(1, dao.marcarAvisado(oleo.id, Urgencia.VENCIDO, quando = 8))
        assertEquals(Urgencia.VENCIDO, dao.buscar(oleo.id)?.avisadoComo)
        // Item excluído ou resolvido não é marcado: zero linhas, para quem chama saber.
        assertEquals(0, dao.marcarAvisado(excluido.id, Urgencia.VENCIDO, quando = 9))
        assertEquals(0, dao.marcarAvisado(resolvido.id, Urgencia.VENCIDO, quando = 9))
    }

    @Test
    fun planoBaseContaTambemOsExcluidosParaNaoVoltar() = runTest {
        val dao = banco.itemDoPlanoDao()
        val base = ItemDoPlano(motoId = "a", nome = "Óleo", intervaloKm = 5_000, origem = OrigemItem.PLANO_BASE)
        dao.inserir(base)
        assertEquals(1, dao.contarPlanoBase("a"))
        assertEquals(1, dao.marcarExcluido(base.id, quando = 1))
        assertEquals(1, dao.contarPlanoBase("a"))
        assertEquals(0, dao.contarPlanoBase("b"))
    }

    @Test
    fun ocorrenciasDaMotoLeOsRegistrosDoDiarioSoDoTipoOcorrenciaAtivos() = runTest {
        val registros = banco.registroDao()
        fun registro(titulo: String, tipo: TipoRegistro, motoId: String = "a", ocorrencia: TipoOcorrencia? = null) =
            Registro(motoId = motoId, tipo = tipo, titulo = titulo, dataEm = 1, ocorrencia = ocorrencia)
        val queda = registro("Tombo", TipoRegistro.OCORRENCIA, ocorrencia = TipoOcorrencia.QUEDA)
        val apagada = registro("Apagada", TipoRegistro.OCORRENCIA, ocorrencia = TipoOcorrencia.BARULHO)
        listOf(
            queda,
            apagada,
            registro("Nota", TipoRegistro.NOTA),
            registro("Outra moto", TipoRegistro.OCORRENCIA, motoId = "b", ocorrencia = TipoOcorrencia.QUEDA),
        ).forEach { registros.inserir(it) }
        assertEquals(1, registros.marcarExcluido(apagada.id, quando = 2))

        val achadas = banco.itemDoPlanoDao().ocorrenciasDaMoto("a")
        assertEquals(listOf("Tombo"), achadas.map { it.titulo })
        assertEquals(TipoOcorrencia.QUEDA, achadas.single().ocorrencia)
    }
}
