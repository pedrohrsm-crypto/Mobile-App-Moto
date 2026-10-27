package com.pedrohrsm.diariodebordo.plano.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.plano.dominio.Urgencia
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDoPlanoDao {
    /** Itens periódicos e pendências ainda abertas. */
    @Query(
        "SELECT * FROM itens_plano WHERE motoId = :motoId AND excluidoEm IS NULL AND resolvidoEm IS NULL " +
            "ORDER BY nome COLLATE NOCASE",
    )
    fun observarDaMoto(motoId: String): Flow<List<ItemDoPlano>>

    @Query("SELECT * FROM itens_plano WHERE excluidoEm IS NULL AND resolvidoEm IS NULL")
    suspend fun abertos(): List<ItemDoPlano>

    @Query("SELECT * FROM itens_plano WHERE id = :id AND excluidoEm IS NULL")
    suspend fun buscar(id: String): ItemDoPlano?

    @Query("SELECT COUNT(*) FROM itens_plano WHERE registroOrigemId = :registroId AND excluidoEm IS NULL")
    suspend fun contarDaOcorrencia(registroId: String): Int

    /** Conta também os excluídos: quem apagou o plano-base de propósito não o recebe de volta. */
    @Query("SELECT COUNT(*) FROM itens_plano WHERE motoId = :motoId AND origem = 'PLANO_BASE'")
    suspend fun contarPlanoBase(motoId: String): Int

    /** Leitura do diário: as ocorrências da moto, de onde saem as pendências de inspeção. */
    @Query("SELECT * FROM registros WHERE motoId = :motoId AND tipo = 'OCORRENCIA' AND excluidoEm IS NULL")
    suspend fun ocorrenciasDaMoto(motoId: String): List<Registro>

    @Insert
    suspend fun inserir(item: ItemDoPlano)

    @Update
    suspend fun atualizar(item: ItemDoPlano): Int

    /** Só a urgência avisada muda: não sobrescreve uma edição que a pessoa fez enquanto o worker rodava. */
    @Query(
        "UPDATE itens_plano SET avisadoComo = :urgencia, alteradoEm = :quando " +
            "WHERE id = :id AND excluidoEm IS NULL AND resolvidoEm IS NULL",
    )
    suspend fun marcarAvisado(id: String, urgencia: Urgencia, quando: Long): Int

    @Query("UPDATE itens_plano SET excluidoEm = :quando, alteradoEm = :quando WHERE id = :id AND excluidoEm IS NULL")
    suspend fun marcarExcluido(id: String, quando: Long): Int
}
