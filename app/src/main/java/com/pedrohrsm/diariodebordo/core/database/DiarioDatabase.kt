package com.pedrohrsm.diariodebordo.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistro
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.RegistroDao
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoDao

/**
 * Fonte da verdade do app (offline-first). Decisão #12: nenhuma versão foi publicada, então as tabelas
 * entram na versão 1 aberta, com o schema de app/schemas/ commitado a cada mudança. Depois da primeira
 * publicação, ao mudar uma entidade: subir a [version], escrever a Migration testada e commitar o schema novo.
 */
@Database(
    entities = [Moto::class, LeituraHodometro::class, Registro::class, FotoRegistro::class],
    version = 1,
    exportSchema = true,
)
abstract class DiarioDatabase : RoomDatabase() {
    abstract fun motoDao(): MotoDao
    abstract fun leituraHodometroDao(): LeituraHodometroDao
    abstract fun registroDao(): RegistroDao
    abstract fun fotoRegistroDao(): FotoRegistroDao

    companion object {
        const val NOME = "diario.db"
    }
}
