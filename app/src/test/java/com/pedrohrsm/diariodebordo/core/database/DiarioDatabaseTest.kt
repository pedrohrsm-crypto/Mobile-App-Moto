package com.pedrohrsm.diariodebordo.core.database

import androidx.room.Room
import com.pedrohrsm.diariodebordo.garagem.data.Moto
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

    @Test
    fun abreNaVersao1ComATabelaDeMotos() {
        val db = banco.openHelper.writableDatabase
        db.query("PRAGMA user_version").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'motos'").use {
            assertEquals(1, it.count)
        }
    }

    @Test
    fun gravaELeUmaMotoDeVolta() = runTest {
        val moto = Moto(apelido = "Bandida", marca = "Harley-Davidson", modelo = "Sportster 883", ano = 2012)
        banco.motoDao().inserir(moto)
        assertEquals(listOf(moto), banco.motoDao().observarAtivos().first())
    }
}
