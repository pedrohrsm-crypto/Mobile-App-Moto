package com.pedrohrsm.diariodebordo.garagem.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "motos")
data class Moto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val apelido: String,
    val marca: String,
    val modelo: String,
    val ano: Int?,
    val hodometroKm: Int,
)
