package com.pedrohrsm.diariodebordo.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 1 → 2 (#88): `rotas.pontasJaCortadasM`, o corte das pontas que a polilinha salva já sofreu. Toda rota da versão 1
 * fica com 0 (pontas reais) e continua perdendo as pontas na exportação, como antes: nenhuma rota sai mais exposta.
 */
val MIGRACAO_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `rotas` ADD COLUMN `pontasJaCortadasM` INTEGER NOT NULL DEFAULT 0")
    }
}

/** Todas as Migrations, da mais antiga à mais nova. O app abre um banco de toda versão já instalada. */
val MIGRACOES = arrayOf(MIGRACAO_1_2)
