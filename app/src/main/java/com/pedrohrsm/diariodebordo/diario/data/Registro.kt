package com.pedrohrsm.diariodebordo.diario.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TipoRegistro { MANUTENCAO, ABASTECIMENTO, OCORRENCIA, ROLE, NOTA }

/** Tipo de ocorrência: define os itens de inspeção que ela cria no plano. */
enum class TipoOcorrencia { QUEDA, BARULHO, VAZAMENTO, OUTRA }

/**
 * Uma linha do diário. Campos opcionais valem conforme o [tipo] (litros só no abastecimento etc.).
 * Dinheiro em centavos, volume em mililitros, instantes em milissegundos UTC.
 * Campos de controle conforme docs/offline-first.md, preenchidos pelo [RegistroRepositorio].
 */
@Entity(
    tableName = "registros",
    indices = [Index(value = ["motoId", "dataEm"]), Index("itemPlanoId"), Index("rotaId")],
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
    /** Item do plano cumprido por esta manutenção. */
    val itemPlanoId: String? = null,
    /** Rota gravada neste rolê. */
    val rotaId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val criadoEm: Long = 0,
    val alteradoEm: Long = 0,
    val fusoHorario: String = "",
    val excluidoEm: Long? = null,
    val pendenteSync: Boolean = false,
)
