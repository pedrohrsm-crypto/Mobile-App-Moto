package com.pedrohrsm.diariodebordo.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pedrohrsm.diariodebordo.backup.data.BackupDao
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistro
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.RegistroDao
import com.pedrohrsm.diariodebordo.garagem.data.ExclusaoDeMotoDao
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
 * Fonte da verdade do app (offline-first). Mudança de entidade: [version] + 1, Migration em [MIGRACOES] testada
 * e o schema novo exportado em app/schemas/ e commitado (CLAUDE.md). A versão 1 ficou aberta até a decisão #12; a 2
 * (#88) é a primeira com Migration, porque o APK de release assinado (#63) já pode ter um diário de verdade.
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
    version = 2,
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
    abstract fun exclusaoDeMotoDao(): ExclusaoDeMotoDao

    companion object {
        const val NOME = "diario.db"
    }
}
