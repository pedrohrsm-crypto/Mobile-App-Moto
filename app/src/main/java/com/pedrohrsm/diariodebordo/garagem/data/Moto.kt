package com.pedrohrsm.diariodebordo.garagem.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** Categoria define o plano-base de manutenção (plano de produto, seção Produto). */
enum class CategoriaMoto { CUSTOM, STREET, TRAIL, SCOOTER }

/**
 * Os campos de controle (id, criadoEm, alteradoEm, fusoHorario, excluidoEm, pendenteSync)
 * seguem docs/offline-first.md e são preenchidos pelo [MotoRepositorio], nunca pela tela.
 * O km atual não fica aqui: é a leitura de hodômetro mais recente (tabela futura).
 */
@Entity(tableName = "motos")
data class Moto(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val apelido: String,
    val marca: String,
    val modelo: String,
    val ano: Int? = null,
    val categoria: CategoriaMoto? = null,
    val criadoEm: Long = 0,
    val alteradoEm: Long = 0,
    val fusoHorario: String = "",
    val excluidoEm: Long? = null,
    val pendenteSync: Boolean = false,
    val corPlantada: String? = null,
)
