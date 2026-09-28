package com.pedrohrsm.diariodebordo.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoDao

/**
 * Fonte da verdade do app (offline-first). Nenhuma versão foi publicada ainda, então as tabelas do MVP
 * entram todas na versão 1 (item 0.7 do plano de ação). Depois da primeira publicação, ao mudar uma
 * entidade: subir a [version], escrever a Migration e commitar o schema gerado em app/schemas/.
 */
@Database(
    entities = [Moto::class, LeituraHodometro::class],
    version = 1,
    exportSchema = true,
)
abstract class DiarioDatabase : RoomDatabase() {
    abstract fun motoDao(): MotoDao
    abstract fun leituraHodometroDao(): LeituraHodometroDao

    companion object {
        const val NOME = "diario.db"
    }
}
