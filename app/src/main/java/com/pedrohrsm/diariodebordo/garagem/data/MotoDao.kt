package com.pedrohrsm.diariodebordo.garagem.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MotoDao {
    @Query("SELECT * FROM motos ORDER BY apelido")
    fun observarTodas(): Flow<List<Moto>>

    @Insert
    suspend fun inserir(moto: Moto): Long

    /** Retorna quantas linhas mudaram, para quem chama conferir (regra do framework). */
    @Update
    suspend fun atualizar(moto: Moto): Int
}
