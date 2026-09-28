package com.pedrohrsm.diariodebordo.plano.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
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

    @Insert
    suspend fun inserir(item: ItemDoPlano)

    @Update
    suspend fun atualizar(item: ItemDoPlano): Int

    @Query("UPDATE itens_plano SET excluidoEm = :quando, alteradoEm = :quando WHERE id = :id AND excluidoEm IS NULL")
    suspend fun marcarExcluido(id: String, quando: Long): Int
}
