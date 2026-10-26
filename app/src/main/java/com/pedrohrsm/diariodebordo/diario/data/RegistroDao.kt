package com.pedrohrsm.diariodebordo.diario.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroDao {
    @Query("SELECT * FROM registros WHERE motoId = :motoId AND excluidoEm IS NULL ORDER BY dataEm DESC")
    fun observarDaMoto(motoId: String): Flow<List<Registro>>

    @Query("SELECT * FROM registros WHERE id = :id AND excluidoEm IS NULL")
    suspend fun buscar(id: String): Registro?

    @Insert
    suspend fun inserir(item: Registro)

    @Update
    suspend fun atualizar(item: Registro): Int

    @Query("UPDATE registros SET excluidoEm = :quando, alteradoEm = :quando WHERE id = :id AND excluidoEm IS NULL")
    suspend fun marcarExcluido(id: String, quando: Long): Int
}

@Dao
interface FotoRegistroDao {
    @Query("SELECT * FROM fotos_registro WHERE registroId = :registroId AND excluidoEm IS NULL ORDER BY ordem")
    suspend fun doRegistro(registroId: String): List<FotoRegistro>

    @Insert
    suspend fun inserir(item: FotoRegistro)

    @Update
    suspend fun atualizar(item: FotoRegistro): Int

    @Query("UPDATE fotos_registro SET excluidoEm = :quando, alteradoEm = :quando WHERE id = :id AND excluidoEm IS NULL")
    suspend fun marcarExcluido(id: String, quando: Long): Int
}
