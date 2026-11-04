package com.pedrohrsm.diariodebordo.garagem.data

import androidx.room.Dao
import androidx.room.Query

/**
 * Consultas da exclusão de uma moto com tudo o que é dela (#65): conta e marca `excluidoEm` por moto nas
 * tabelas do diário, do plano, das rotas e do hodômetro. Só a [ExclusaoDeMoto] usa, dentro de uma transação.
 * As fotos não têm motoId: saem pelos registros da moto, inclusive os já excluídos.
 */
@Dao
interface ExclusaoDeMotoDao {
    @Query("SELECT COUNT(*) FROM registros WHERE motoId = :motoId AND excluidoEm IS NULL")
    suspend fun contarRegistros(motoId: String): Int

    @Query(
        "SELECT arquivo FROM fotos_registro WHERE excluidoEm IS NULL " +
            "AND registroId IN (SELECT id FROM registros WHERE motoId = :motoId)",
    )
    suspend fun arquivosDasFotos(motoId: String): List<String>

    @Query("SELECT COUNT(*) FROM itens_plano WHERE motoId = :motoId AND excluidoEm IS NULL")
    suspend fun contarItensDoPlano(motoId: String): Int

    @Query("SELECT COUNT(*) FROM rotas WHERE motoId = :motoId AND excluidoEm IS NULL AND emGravacao = 0")
    suspend fun contarRotas(motoId: String): Int

    @Query("SELECT COUNT(*) FROM rotas WHERE motoId = :motoId AND excluidoEm IS NULL AND emGravacao = 1")
    suspend fun contarRotasEmGravacao(motoId: String): Int

    @Query("SELECT COUNT(*) FROM leituras_hodometro WHERE motoId = :motoId AND excluidoEm IS NULL")
    suspend fun contarLeituras(motoId: String): Int

    @Query(
        "UPDATE fotos_registro SET excluidoEm = :quando, alteradoEm = :quando WHERE excluidoEm IS NULL " +
            "AND registroId IN (SELECT id FROM registros WHERE motoId = :motoId)",
    )
    suspend fun marcarFotos(motoId: String, quando: Long): Int

    @Query(
        "UPDATE registros SET excluidoEm = :quando, alteradoEm = :quando WHERE motoId = :motoId AND excluidoEm IS NULL",
    )
    suspend fun marcarRegistros(motoId: String, quando: Long): Int

    @Query(
        "UPDATE itens_plano SET excluidoEm = :quando, alteradoEm = :quando WHERE motoId = :motoId AND excluidoEm IS NULL",
    )
    suspend fun marcarItensDoPlano(motoId: String, quando: Long): Int

    /** O traçado sai junto, como na exclusão de uma rota só (risco #23): a linha fica, o trajeto não. */
    @Query(
        "UPDATE rotas SET excluidoEm = :quando, alteradoEm = :quando, polilinha = '' " +
            "WHERE motoId = :motoId AND excluidoEm IS NULL AND emGravacao = 0",
    )
    suspend fun marcarRotas(motoId: String, quando: Long): Int

    @Query(
        "UPDATE leituras_hodometro SET excluidoEm = :quando, alteradoEm = :quando " +
            "WHERE motoId = :motoId AND excluidoEm IS NULL",
    )
    suspend fun marcarLeituras(motoId: String, quando: Long): Int

    @Query("UPDATE motos SET excluidoEm = :quando, alteradoEm = :quando WHERE id = :motoId AND excluidoEm IS NULL")
    suspend fun marcarMoto(motoId: String, quando: Long): Int
}
