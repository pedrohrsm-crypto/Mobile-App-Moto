package com.pedrohrsm.diariodebordo.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoDao

/**
 * Fonte da verdade do app (offline-first). Ao mudar uma entidade: subir a [version],
 * escrever a Migration e commitar o schema gerado em app/schemas/.
 */
@Database(
    entities = [Moto::class],
    version = 1,
    exportSchema = true,
)
abstract class DiarioDatabase : RoomDatabase() {
    abstract fun motoDao(): MotoDao

    companion object {
        const val NOME = "diario.db"
    }
}
