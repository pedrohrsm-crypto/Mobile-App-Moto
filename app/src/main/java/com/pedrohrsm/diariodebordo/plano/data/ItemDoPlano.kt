package com.pedrohrsm.diariodebordo.plano.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pedrohrsm.diariodebordo.plano.dominio.Intervalo
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import java.util.UUID

enum class OrigemItem { PLANO_BASE, USUARIO, OCORRENCIA }

/**
 * Item do plano de manutenção. Periódico (tem intervalo em km e/ou dias) ou pendência criada por
 * uma ocorrência (sem intervalo; some quando resolvida). Campos de controle conforme
 * docs/offline-first.md, preenchidos pelo [PlanoRepositorio].
 */
@Entity(
    tableName = "itens_plano",
    indices = [Index("motoId"), Index("registroOrigemId")],
)
data class ItemDoPlano(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val motoId: String,
    /** Chave estável dos itens que o código procura (ex.: troca de óleo); null nos itens só da pessoa. */
    val chave: String? = null,
    val nome: String,
    val intervaloKm: Int? = null,
    val intervaloDias: Int? = null,
    /** Referência da conta: o km e o instante de quando o item foi criado ou feito pela última vez. */
    val ultimaVezKm: Int? = null,
    val ultimaVezEm: Long? = null,
    /** Quando a pessoa marcou o item como feito pela última vez; null = a conta ainda parte da criação do plano. */
    val feitoEm: Long? = null,
    val origem: OrigemItem = OrigemItem.USUARIO,
    /** Ocorrência que criou esta pendência. */
    val registroOrigemId: String? = null,
    val resolvidoEm: Long? = null,
    /** Maior urgência já avisada por lembrete; limpa quando o item é feito ou o intervalo muda (erro #26). */
    val avisadoComo: Urgencia? = null,
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
