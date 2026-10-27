package com.pedrohrsm.diariodebordo.core.di

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Decisão #12: sem Migration, só o debug recria o banco. Simula um banco gravado por outra versão do schema
 * (user_version diferente da do app) e confere o que cada build faz com ele.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseModuleTest {

    private val contexto: Application = RuntimeEnvironment.getApplication()
    private val nome = "teste-migracao.db"

    @Before
    fun gravarBancoDeOutraVersao() = runTest {
        val banco = DatabaseModule.criarBanco(contexto, nome, debug = true)
        banco.motoDao().inserir(Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "883"))
        banco.close()
        val caminho = contexto.getDatabasePath(nome).path
        SQLiteDatabase.openDatabase(caminho, null, SQLiteDatabase.OPEN_READWRITE).use { it.version = 2 }
    }

    @After
    fun apagar() {
        contexto.deleteDatabase(nome)
    }

    @Test
    fun releaseSemMigrationFalhaEmVezDeApagarODiario() {
        val banco = DatabaseModule.criarBanco(contexto, nome, debug = false)
        try {
            banco.openHelper.writableDatabase
            fail("o release abriu um banco de outra versão sem Migration")
        } catch (esperado: IllegalStateException) {
            assertTrue(esperado.message.orEmpty().contains("migration", ignoreCase = true))
        } finally {
            banco.close()
        }
        // O arquivo continua lá, com a moto: nada foi apagado.
        val caminho = contexto.getDatabasePath(nome).path
        SQLiteDatabase.openDatabase(caminho, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT COUNT(*) FROM motos", null).use {
                it.moveToFirst()
                assertEquals(1, it.getInt(0))
            }
        }
    }

    @Test
    fun debugRecriaOBancoNaVersaoDoApp() = runTest {
        val banco = DatabaseModule.criarBanco(contexto, nome, debug = true)
        try {
            assertEquals(1, banco.openHelper.writableDatabase.version)
            assertTrue(banco.motoDao().observarAtivos().first().isEmpty())
        } finally {
            banco.close()
        }
    }

    /**
     * O caso do dia a dia da v1 aberta: o schema muda e a versão continua 1. O Room não trata isso como
     * migração (nem o fallback do debug recria): ele recusa abrir. No aparelho de teste, limpar os dados do app.
     */
    @Test
    fun schemaDiferenteNaMesmaVersaoRecusaAbrirAteNoDebug() {
        val caminho = contexto.getDatabasePath(nome).path
        SQLiteDatabase.openDatabase(caminho, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.version = 1
            it.execSQL("UPDATE room_master_table SET identity_hash = 'schema-de-outro-build'")
        }
        val banco = DatabaseModule.criarBanco(contexto, nome, debug = true)
        try {
            banco.openHelper.writableDatabase
            fail("abriu um banco com o schema de outro build")
        } catch (esperado: IllegalStateException) {
            assertTrue(esperado.message.orEmpty(), esperado.message.orEmpty().contains("verify the data integrity"))
        } finally {
            banco.close()
        }
    }
}
