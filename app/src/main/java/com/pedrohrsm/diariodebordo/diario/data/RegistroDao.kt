package com.pedrohrsm.diariodebordo.diario.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface RegistroDao {
    /** Diário paginado, do mais recente ao mais antigo (PLANEJAMENTO §4). [tipo] null traz todos. */
    @Query(
        "SELECT * FROM registros WHERE motoId = :motoId AND excluidoEm IS NULL " +
            "AND (:tipo IS NULL OR tipo = :tipo) ORDER BY dataEm DESC, criadoEm DESC",
    )
    fun paginarDaMoto(motoId: String, tipo: String?): PagingSource<Int, Registro>

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

    /**
     * Arquivos de todas as fotos ativas, de todas as motos: a varredura de órfãs (#94) não apaga nenhum deles.
     * Consulta única, não `Flow`: se falhar, a exceção sobe e a varredura não apaga nada.
     */
    @Query("SELECT DISTINCT arquivo FROM fotos_registro WHERE excluidoEm IS NULL")
    suspend fun arquivosAtivos(): List<String>
}
