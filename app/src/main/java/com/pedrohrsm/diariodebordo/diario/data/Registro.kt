package com.pedrohrsm.diariodebordo.diario.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TipoRegistro { MANUTENCAO, ABASTECIMENTO, OCORRENCIA, ROLE, NOTA }

/** O que aconteceu numa ocorrência. */
enum class TipoOcorrencia { QUEDA, BARULHO, VAZAMENTO, OUTRA }

/**
 * Uma linha do diário. Campos opcionais valem conforme o [tipo] (litros só no abastecimento etc.).
 * Dinheiro em centavos, volume em mililitros, instantes em milissegundos UTC; "sem dado" é nulo, nunca zero.
 * Campos de controle conforme docs/offline-first.md, preenchidos pelo [RegistroRepositorio].
 * O índice (motoId, dataEm) atende a lista paginada do diário (PLANEJAMENTO §4).
 */
@Entity(
    tableName = "registros",
    indices = [Index(value = ["motoId", "dataEm"])],
)
data class Registro(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val motoId: String,
    val tipo: TipoRegistro,
    val titulo: String,
    val descricao: String = "",
    /** Quando o fato aconteceu (pode ser retroativo). */
    val dataEm: Long,
    val hodometroKm: Int? = null,
    val valorCentavos: Long? = null,
    val litrosMl: Long? = null,
    val tanqueCheio: Boolean = false,
    val ocorrencia: TipoOcorrencia? = null,
    val criadoEm: Long = 0,
    val alteradoEm: Long = 0,
    val fusoHorario: String = "",
    val excluidoEm: Long? = null,
    val pendenteSync: Boolean = false,
)
