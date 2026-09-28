package com.pedrohrsm.diariodebordo.plano.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pedrohrsm.diariodebordo.plano.dominio.Intervalo
import java.util.UUID

enum class OrigemItem { PLANO_BASE, USUARIO, OCORRENCIA }

/**
 * Item do plano de manutenção. Periódico (tem intervalo em km e/ou dias) ou pendência criada por
 * uma ocorrência (sem intervalo; some quando resolvida). Campos de controle conforme
 * docs/offline-first.md, preenchidos pelo [PlanoRepositorio].
 */
@Entity(tableName = "itens_plano", indices = [Index("motoId"), Index("registroOrigemId")])
data class ItemDoPlano(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val motoId: String,
    val nome: String,
    val intervaloKm: Int? = null,
    val intervaloDias: Int? = null,
    /** Referência da conta: última vez feito, ou o cadastro da moto quando nunca foi registrado. */
    val ultimaVezKm: Int? = null,
    val ultimaVezEm: Long? = null,
    /** Registro do diário que cumpriu o item pela última vez; null = conta desde o cadastro. */
    val ultimoRegistroId: String? = null,
    val origem: OrigemItem = OrigemItem.USUARIO,
    /** Ocorrência que criou esta pendência. */
    val registroOrigemId: String? = null,
    val resolvidoEm: Long? = null,
    /** Último aviso de lembrete para a situação atual; limpo quando o item é feito. */
    val avisadoEm: Long? = null,
    val criadoEm: Long = 0,
    val alteradoEm: Long = 0,
    val fusoHorario: String = "",
    val excluidoEm: Long? = null,
    val pendenteSync: Boolean = false,
)

// Fora da entidade para o Room não tentar mapear como coluna.
val ItemDoPlano.pendencia: Boolean get() = origem == OrigemItem.OCORRENCIA

/** Null para pendência (sem intervalo). */
val ItemDoPlano.intervalo: Intervalo?
    get() = if (intervaloKm == null && intervaloDias == null) null else Intervalo(intervaloKm, intervaloDias)
