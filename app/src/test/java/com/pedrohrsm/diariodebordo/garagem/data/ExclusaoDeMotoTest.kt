package com.pedrohrsm.diariodebordo.garagem.data

import androidx.room.Room
import com.pedrohrsm.diariodebordo.core.database.DiarioDatabase
import com.pedrohrsm.diariodebordo.core.database.TransacaoRoom
import com.pedrohrsm.diariodebordo.core.premium.Premium
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistro
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.diario.ui.GuardaFotosFalsa
import com.pedrohrsm.diariodebordo.garagem.dominio.ContagemDaMoto
import com.pedrohrsm.diariodebordo.garagem.dominio.GravacaoEmAndamentoException
import com.pedrohrsm.diariodebordo.garagem.dominio.LimiteDeMotosException
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import com.pedrohrsm.diariodebordo.rota.data.Rota
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * A exclusão da moto com o banco de verdade (Room no SQLite do Robolectric): as consultas do [ExclusaoDeMotoDao]
 * e o tudo-ou-nada da [TransacaoRoom] passam pelo mesmo caminho do app (#65).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExclusaoDeMotoTest {

    private val instante = Instant.parse("2026-11-03T12:00:00Z")
    private val relogio = Clock.fixed(instante, ZoneId.of("America/Sao_Paulo"))
    private val banco: DiarioDatabase =
        Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), DiarioDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val fotos = GuardaFotosFalsa()
    private val agendador = AgendadorFalso()
    private val motos = MotoRepositorio(banco.motoDao(), relogio)

    private fun exclusao(dao: ExclusaoDeMotoDao = banco.exclusaoDeMotoDao()) =
        ExclusaoDeMoto(dao, TransacaoRoom(banco), fotos, agendador, relogio)

    @After
    fun fechar() = banco.close()

    /** Cada moto com 2 registros (o primeiro com 2 fotos), 1 item do plano, 1 rota salva e 1 leitura. */
    private suspend fun povoar(motoId: String, apelido: String) {
        banco.motoDao().inserir(Moto(id = motoId, apelido = apelido, marca = "Honda", modelo = "CG"))
        for (n in 1..2) {
            banco.registroDao().inserir(
                Registro(id = "$motoId-r$n", motoId = motoId, tipo = TipoRegistro.NOTA, titulo = "Nota", dataEm = 1),
            )
        }
        for (n in 1..2) {
            banco.fotoRegistroDao()
                .inserir(
                    FotoRegistro(id = "$motoId-f$n", registroId = "$motoId-r1", arquivo = "$motoId-$n.jpg", ordem = n),
                )
        }
        banco.itemDoPlanoDao().inserir(
            ItemDoPlano(id = "$motoId-i", motoId = motoId, nome = "Óleo", intervaloKm = 3000),
        )
        banco.rotaDao().inserir(
            Rota(id = "$motoId-rota", motoId = motoId, iniciadaEm = 1, polilinha = "_p~iF~ps|U", emGravacao = false),
        )
        banco.leituraHodometroDao().inserir(LeituraHodometro(id = "$motoId-l", motoId = motoId, km = 1000))
    }

    private fun excluidos(tabela: String): Set<String> =
        banco.openHelper.readableDatabase.query("SELECT id FROM $tabela WHERE excluidoEm IS NOT NULL").use {
            buildSet { while (it.moveToNext()) add(it.getString(0)) }
        }

    private val tabelas = listOf("motos", "registros", "fotos_registro", "itens_plano", "rotas", "leituras_hodometro")

    private fun nadaExcluido() = tabelas.forEach { assertEquals(it, emptySet<String>(), excluidos(it)) }

    @Test
    fun contarDizOQueVaiSumirSoDaquelaMoto() = runTest {
        povoar("a", "Bandida")
        povoar("b", "Estradeira")
        assertEquals(
            ContagemDaMoto(registros = 2, fotos = 2, itensDoPlano = 1, rotas = 1, leituras = 1),
            exclusao().contar("a"),
        )
    }

    @Test
    fun excluirMarcaAMotoETudoDelaSemTocarNaOutra() = runTest {
        povoar("a", "Bandida")
        povoar("b", "Estradeira")

        val resultado = exclusao().excluir("a")

        assertEquals(setOf("a"), excluidos("motos"))
        assertEquals(setOf("a-r1", "a-r2"), excluidos("registros"))
        assertEquals(setOf("a-f1", "a-f2"), excluidos("fotos_registro"))
        assertEquals(setOf("a-i"), excluidos("itens_plano"))
        assertEquals(setOf("a-rota"), excluidos("rotas"))
        assertEquals(setOf("a-l"), excluidos("leituras_hodometro"))
        // O traçado da rota excluída sai junto (risco #23); o da outra moto fica.
        banco.openHelper.readableDatabase.query("SELECT id, polilinha FROM rotas ORDER BY id").use {
            val polilinhas = buildMap { while (it.moveToNext()) put(it.getString(0), it.getString(1)) }
            assertEquals(mapOf("a-rota" to "", "b-rota" to "_p~iF~ps|U"), polilinhas)
        }
        // Os arquivos das fotos saem do armazenamento; os da outra moto, não.
        assertEquals(listOf("a-1.jpg", "a-2.jpg"), fotos.apagadas.sorted())
        assertEquals(emptyList<String>(), resultado.fotosQueFicaram)
        assertEquals(1, agendador.recalculos)
        assertEquals(listOf("b"), motos.observarAtivos().first().map { it.id })
    }

    @Test
    fun falhaNoMeioDaTransacaoNaoExcluiNada() = runTest {
        povoar("a", "Bandida")
        val real = banco.exclusaoDeMotoDao()
        // Fotos, registros e plano já marcados quando as rotas falham; a moto seria marcada por último.
        val quebrado = object : ExclusaoDeMotoDao by real {
            override suspend fun marcarRotas(motoId: String, quando: Long): Int = throw IOException("disco cheio")
        }
        try {
            exclusao(quebrado).excluir("a")
            fail("a falha nas rotas devia chegar a quem chamou")
        } catch (e: IOException) {
            assertEquals("disco cheio", e.message)
        }
        nadaExcluido()
        assertEquals(emptyList<String>(), fotos.apagadas)
        assertEquals(0, agendador.recalculos)
    }

    @Test
    fun linhasQueNaoBatemComAContagemDesfazemTudo() = runTest {
        povoar("a", "Bandida")
        val real = banco.exclusaoDeMotoDao()
        // Uma foto a menos marcada do que a contagem leu: a escrita não confere e nada fica excluído.
        val quebrado = object : ExclusaoDeMotoDao by real {
            override suspend fun marcarFotos(motoId: String, quando: Long): Int = real.marcarFotos(motoId, quando) - 1
        }
        try {
            exclusao(quebrado).excluir("a")
            fail("linhas que não batem deviam falhar")
        } catch (e: IllegalStateException) {
            assertTrue(e.message, e.message!!.contains("fotos"))
        }
        nadaExcluido()
        assertEquals(emptyList<String>(), fotos.apagadas)
    }

    @Test
    fun comRotaGravandoRecusaSemTocarEmNada() = runTest {
        povoar("a", "Bandida")
        banco.rotaDao().inserir(Rota(id = "gravando", motoId = "a", iniciadaEm = 2, emGravacao = true))
        try {
            exclusao().excluir("a")
            fail("excluir com rota em gravação devia ser recusado")
        } catch (_: GravacaoEmAndamentoException) {
        }
        nadaExcluido()
    }

    @Test
    fun motoExcluidaSaiDaMotoAtual() = runTest {
        povoar("a", "Bandida")
        povoar("b", "Estradeira")
        val motoAtual = MotoAtual(motos)
        motoAtual.selecionar("a")
        assertEquals("a", motoAtual.moto.first()?.id)

        exclusao().excluir("a")
        assertEquals("b", motoAtual.moto.first()?.id)

        exclusao().excluir("b")
        assertNull(motoAtual.moto.first())
    }

    /**
     * O limite do plano grátis conta só as motos ativas (#72, separado do teste da moto atual). A fixture tem uma
     * moto só, no limite: antes de excluir o cadastro é recusado; depois, cabe uma nova.
     */
    @Test
    fun motoExcluidaSaiDoLimiteDeMotos() = runTest {
        povoar("a", "Bandida")
        val hodometro =
            HodometroRepositorio(
                banco.leituraHodometroDao(),
                TransacaoRoom(banco),
                relogio,
                AgendadorFalso(),
                PlanoRepositorio(banco.itemDoPlanoDao(), TransacaoRoom(banco), relogio, AgendadorFalso()),
            )
        val cadastro = CadastroMoto(motos, hodometro, TransacaoRoom(banco), Premium(habilitado = false))
        val nova = Moto(apelido = "Nova", marca = "Yamaha", modelo = "Fazer")
        try {
            cadastro.cadastrar(nova, kmInicial = 10)
            fail("com uma moto ativa, o plano grátis devia recusar a segunda")
        } catch (_: LimiteDeMotosException) {
        }

        exclusao().excluir("a")
        val criada = cadastro.cadastrar(nova, kmInicial = 10)
        assertEquals(listOf(criada.id), motos.observarAtivos().first().map { it.id })
    }

    @Test
    fun falhaAoApagarFotoDepoisDoCommitNaoDesfazAExclusao() = runTest {
        povoar("a", "Bandida")
        fotos.falhaAoApagar = { arquivo -> if (arquivo == "a-1.jpg") SecurityException("sem permissão") else null }

        val resultado = exclusao().excluir("a")

        assertEquals(setOf("a"), excluidos("motos"))
        assertEquals(listOf("a-1.jpg"), resultado.fotosQueFicaram)
        // A falha numa foto não para as outras nem o recálculo dos lembretes.
        assertEquals(listOf("a-2.jpg"), fotos.apagadas)
        assertEquals(1, agendador.recalculos)
        assertTrue(resultado.lembretesRecalculados)
    }

    @Test
    fun falhaAoRecalcularLembretesDepoisDoCommitViraAviso() = runTest {
        povoar("a", "Bandida")
        agendador.falha = IllegalStateException("WorkManager indisponível")

        val resultado = exclusao().excluir("a")

        assertEquals(setOf("a"), excluidos("motos"))
        assertFalse(resultado.lembretesRecalculados)
        assertEquals(emptyList<String>(), resultado.fotosQueFicaram)
    }

    @Test
    fun cancelamentoDepoisDoCommitERelancado() = runTest {
        povoar("a", "Bandida")
        fotos.falhaAoApagar = { CancellationException("a tela saiu") }
        try {
            exclusao().excluir("a")
            fail("o cancelamento da corrotina devia chegar a quem chamou")
        } catch (e: CancellationException) {
            assertEquals("a tela saiu", e.message)
        }
        // O commit já tinha acontecido: o cancelamento não desfaz a exclusão.
        assertEquals(setOf("a"), excluidos("motos"))
    }
}
