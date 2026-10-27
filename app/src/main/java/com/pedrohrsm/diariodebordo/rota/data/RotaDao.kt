package com.pedrohrsm.diariodebordo.rota.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RotaDao {
    @Query(
        "SELECT * FROM rotas WHERE motoId = :motoId AND excluidoEm IS NULL AND emGravacao = 0 " +
            "ORDER BY iniciadaEm DESC",
    )
    fun observarSalvasDaMoto(motoId: String): Flow<List<Rota>>

    @Query("SELECT * FROM rotas WHERE emGravacao = 1 AND excluidoEm IS NULL ORDER BY iniciadaEm DESC LIMIT 1")
    fun observarEmGravacao(): Flow<Rota?>

    @Query("SELECT * FROM rotas WHERE emGravacao = 1 AND excluidoEm IS NULL ORDER BY iniciadaEm DESC LIMIT 1")
    suspend fun emGravacao(): Rota?

    @Query("SELECT * FROM rotas WHERE id = :id AND excluidoEm IS NULL")
    fun observar(id: String): Flow<Rota?>

    @Query("SELECT * FROM rotas WHERE id = :id AND excluidoEm IS NULL")
    suspend fun buscar(id: String): Rota?

    @Insert
    suspend fun inserir(item: Rota)

    @Update
    suspend fun atualizar(item: Rota): Int

    /** Exclusão lógica que também apaga o traçado (risco #23): a linha fica, o trajeto não. */
    @Query(
        "UPDATE rotas SET excluidoEm = :quando, alteradoEm = :quando, polilinha = '' " +
            "WHERE id = :id AND excluidoEm IS NULL",
    )
    suspend fun marcarExcluido(id: String, quando: Long): Int
}

@Dao
interface PontoDeRotaDao {
    @Query("SELECT * FROM pontos_rota WHERE rotaId = :rotaId ORDER BY instante")
    suspend fun daRota(rotaId: String): List<PontoDeRota>

    @Query("SELECT COUNT(*) FROM pontos_rota WHERE rotaId = :rotaId")
    fun observarQuantidade(rotaId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM pontos_rota WHERE rotaId = :rotaId")
    suspend fun quantidade(rotaId: String): Int

    @Insert
    suspend fun inserir(item: PontoDeRota)

    /** Pontos brutos saem de verdade (não é exclusão lógica): não sincronizam nem ficam guardados. */
    @Query("DELETE FROM pontos_rota WHERE rotaId = :rotaId")
    suspend fun apagarDaRota(rotaId: String): Int
}
