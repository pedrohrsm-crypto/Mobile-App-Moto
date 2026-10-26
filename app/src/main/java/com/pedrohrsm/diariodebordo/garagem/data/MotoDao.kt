package com.pedrohrsm.diariodebordo.garagem.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MotoDao {
    @Query("SELECT * FROM motos WHERE excluidoEm IS NULL ORDER BY apelido COLLATE NOCASE")
    fun observarAtivos(): Flow<List<Moto>>

    @Insert
    suspend fun inserir(item: Moto)

    /** Retorna quantas linhas mudaram, para quem chama conferir (regra do framework). */
    @Update
    suspend fun atualizar(item: Moto): Int

    /** Exclusão lógica, para a exclusão também sincronizar no premium. */
    @Query("UPDATE motos SET excluidoEm = :quando, alteradoEm = :quando WHERE id = :id AND excluidoEm IS NULL")
    suspend fun marcarExcluido(id: String, quando: Long): Int
}
