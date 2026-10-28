package com.pedrohrsm.diariodebordo.garagem.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * Cada atualização do hodômetro vira uma linha: o km atual da moto é a maior leitura ativa.
 * Campos de controle conforme docs/offline-first.md, preenchidos pelo [HodometroRepositorio].
 */
@Serializable
@Entity(
    tableName = "leituras_hodometro",
    indices = [Index(value = ["motoId", "km"])],
)
data class LeituraHodometro(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val motoId: String,
    val km: Int,
    /** Quando o km foi lido (pode ser a data de um registro antigo). */
    val lidaEm: Long = 0,
    val criadoEm: Long = 0,
    val alteradoEm: Long = 0,
    val fusoHorario: String = "",
    val excluidoEm: Long? = null,
    val pendenteSync: Boolean = false,
)

/** Resultado agregado: km atual de cada moto. */
data class KmAtual(val motoId: String, val km: Int)
