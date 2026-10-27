package com.pedrohrsm.diariodebordo.diario.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Foto de um registro. O arquivo fica no armazenamento interno do app, em fotos/[arquivo]. */
@Entity(tableName = "fotos_registro", indices = [Index("registroId")])
data class FotoRegistro(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val registroId: String,
    val arquivo: String,
    val ordem: Int,
    val criadoEm: Long = 0,
    val alteradoEm: Long = 0,
    val fusoHorario: String = "",
    val excluidoEm: Long? = null,
    val pendenteSync: Boolean = false,
)
