package com.pedrohrsm.diariodebordo.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pedrohrsm.diariodebordo.backup.data.BackupDao
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistro
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.RegistroDao
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometroDao
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoDao
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlanoDao
import com.pedrohrsm.diariodebordo.rota.data.PontoDeRota
import com.pedrohrsm.diariodebordo.rota.data.PontoDeRotaDao
import com.pedrohrsm.diariodebordo.rota.data.Rota
import com.pedrohrsm.diariodebordo.rota.data.RotaDao

/**
 * Fonte da verdade do app (offline-first). Versão 1 até a primeira publicação (decisão #12): mudança de
 * entidade fica na versão 1, com o schema de app/schemas/1.json regerado e commitado. Depois da primeira
 * publicação: [version] + 1, Migration testada e o schema novo commitado.
 */
@Database(
    entities = [
        Moto::class,
        LeituraHodometro::class,
        Registro::class,
        FotoRegistro::class,
        ItemDoPlano::class,
        Rota::class,
        PontoDeRota::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class DiarioDatabase : RoomDatabase() {
    abstract fun motoDao(): MotoDao
    abstract fun leituraHodometroDao(): LeituraHodometroDao
    abstract fun registroDao(): RegistroDao
    abstract fun fotoRegistroDao(): FotoRegistroDao
    abstract fun rotaDao(): RotaDao
    abstract fun pontoDeRotaDao(): PontoDeRotaDao
    abstract fun itemDoPlanoDao(): ItemDoPlanoDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val NOME = "diario.db"
    }
}
