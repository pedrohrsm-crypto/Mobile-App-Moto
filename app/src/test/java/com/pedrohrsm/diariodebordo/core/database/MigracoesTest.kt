package com.pedrohrsm.diariodebordo.core.database

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import com.pedrohrsm.diariodebordo.core.di.DatabaseModule
import com.pedrohrsm.diariodebordo.rota.data.Rota
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * (e) do #96: o app abre um banco da versão 1 sem perder dado. O banco é montado com o SQL do schema 1 exportado
 * (app/schemas/.../1.json), como o app da versão 1 o deixou no aparelho, e aberto pelo mesmo [DatabaseModule] do
 * app. Ao abrir, o Room roda as [MIGRACOES] e confere o resultado contra o schema da versão atual: coluna a mais,
 * a menos ou com outro tipo faz a abertura falhar.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigracoesTest {

    private val contexto: Application = RuntimeEnvironment.getApplication()
    private val nome = "teste-versao-1.db"

    private val rotaDaVersao1 = Rota(
        id = "rota",
        motoId = "m",
        iniciadaEm = 1,
        terminadaEm = 2,
        distanciaM = 12_345,
        duracaoS = 600,
        polilinha = "_p~iF~ps|U",
        emGravacao = false,
        criadoEm = 3,
        alteradoEm = 4,
        fusoHorario = "America/Sao_Paulo",
        excluidoEm = null,
        pendenteSync = true,
    )

    @Before
    fun gravarBancoDaVersao1() {
        val schema = Json.parseToJsonElement(File(SCHEMA_1).readText()).jsonObject.getValue("database").jsonObject
        val caminho = contexto.getDatabasePath(nome).also { it.parentFile?.mkdirs() }.path
        SQLiteDatabase.openOrCreateDatabase(caminho, null).use { db ->
            schema.getValue("entities").jsonArray.forEach { entidade ->
                val e = entidade.jsonObject
                val tabela = e.getValue("tableName").jsonPrimitive.content
                val sqls = listOf(e) + e["indices"]?.jsonArray?.map { it.jsonObject }.orEmpty()
                sqls.forEach { db.execSQL(it.getValue("createSql").jsonPrimitive.content.replace(TABELA, tabela)) }
            }
            schema.getValue("setupQueries").jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
            db.execSQL(
                "INSERT INTO motos (id, apelido, marca, modelo, ano, categoria, criadoEm, alteradoEm, fusoHorario, " +
                    "pendenteSync) VALUES ('m', 'Minha moto', 'Honda', 'CG 160', 2022, 'STREET', 1, 2, 'UTC', 0)",
            )
            db.execSQL(
                "INSERT INTO rotas (id, motoId, iniciadaEm, terminadaEm, distanciaM, duracaoS, polilinha, " +
                    "emGravacao, criadoEm, alteradoEm, fusoHorario, excluidoEm, pendenteSync) " +
                    "VALUES ('rota', 'm', 1, 2, 12345, 600, '_p~iF~ps|U', 0, 3, 4, 'America/Sao_Paulo', NULL, 1)",
            )
            db.version = 1
        }
    }

    @After
    fun apagar() {
        contexto.deleteDatabase(nome)
    }

    @Test
    fun bancoDaVersao1AbreNoReleaseSemPerderDado() = runTest {
        conferirQueAbreComOsDados(debug = false)
    }

    /** O debug recriaria o banco se faltasse a Migration (decisão #12): com ela, os dados ficam também no debug. */
    @Test
    fun bancoDaVersao1AbreNoDebugSemPerderDado() = runTest {
        conferirQueAbreComOsDados(debug = true)
    }

    private suspend fun conferirQueAbreComOsDados(debug: Boolean) {
        val banco = DatabaseModule.criarBanco(contexto, nome, debug)
        try {
            assertEquals(VERSAO_ATUAL, banco.openHelper.writableDatabase.version)
            assertEquals(listOf("m"), banco.backupDao().motos().map { it.id })
            assertEquals(listOf(rotaDaVersao1), banco.backupDao().todasAsRotas())
            assertEquals("pontas reais", 0, banco.backupDao().todasAsRotas().single().pontasJaCortadasM)
        } finally {
            banco.close()
        }
    }

    private companion object {
        const val VERSAO_ATUAL = 2
        const val TABELA = "\${TABLE_NAME}"
        const val SCHEMA_1 = "schemas/com.pedrohrsm.diariodebordo.core.database.DiarioDatabase/1.json"
    }
}
