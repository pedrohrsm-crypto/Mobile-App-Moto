package com.pedrohrsm.diariodebordo.rota.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * Rota de um rolê. Durante a gravação os pontos ficam em [PontoDeRota]; ao salvar, só o traçado
 * simplificado fica aqui ([polilinha]) e os pontos brutos são apagados (decisão #17).
 * O índice (motoId, iniciadaEm) atende a lista de rotas da moto (PLANEJAMENTO §4).
 */
@Serializable
@Entity(tableName = "rotas", indices = [Index(value = ["motoId", "iniciadaEm"])])
data class Rota(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val motoId: String,
    val iniciadaEm: Long,
    val terminadaEm: Long? = null,
    val distanciaM: Long = 0,
    val duracaoS: Long = 0,
    val polilinha: String = "",
    val emGravacao: Boolean = true,
    val criadoEm: Long = 0,
    val alteradoEm: Long = 0,
    val fusoHorario: String = "",
    val excluidoEm: Long? = null,
    val pendenteSync: Boolean = false,
)

/** Ponto bruto, só enquanto a rota grava. Nunca sincroniza nem vai no arquivo exportado. */
@Entity(tableName = "pontos_rota", indices = [Index(value = ["rotaId", "instante"])])
data class PontoDeRota(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val rotaId: String,
    val latitude: Double,
    val longitude: Double,
    val instante: Long,
    val precisaoM: Float? = null,
    val criadoEm: Long = 0,
    val alteradoEm: Long = 0,
    val fusoHorario: String = "",
    val excluidoEm: Long? = null,
    val pendenteSync: Boolean = false,
)
