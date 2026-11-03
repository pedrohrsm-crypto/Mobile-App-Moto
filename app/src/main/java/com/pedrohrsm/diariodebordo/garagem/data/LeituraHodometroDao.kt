package com.pedrohrsm.diariodebordo.garagem.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LeituraHodometroDao {
    @Query(
        "SELECT * FROM leituras_hodometro WHERE motoId = :motoId AND excluidoEm IS NULL " +
            "ORDER BY km DESC, lidaEm DESC",
    )
    fun observarDaMoto(motoId: String): Flow<List<LeituraHodometro>>

    /** O km atual da moto é a maior leitura ativa: a regra mora nesta consulta e na de baixo. */
    @Query("SELECT MAX(km) FROM leituras_hodometro WHERE motoId = :motoId AND excluidoEm IS NULL")
    suspend fun kmAtual(motoId: String): Int?

    @Query("SELECT MAX(km) FROM leituras_hodometro WHERE motoId = :motoId AND excluidoEm IS NULL")
    fun observarKmAtual(motoId: String): Flow<Int?>

    @Query("SELECT motoId, MAX(km) AS km FROM leituras_hodometro WHERE excluidoEm IS NULL GROUP BY motoId")
    fun observarKmAtuais(): Flow<List<KmAtual>>

    @Insert
    suspend fun inserir(item: LeituraHodometro)

    /** As leituras ativas da moto na ordem da data (a das vizinhas da regra de edição). */
    @Query(
        "SELECT * FROM leituras_hodometro WHERE motoId = :motoId AND excluidoEm IS NULL " +
            "ORDER BY lidaEm, km, id",
    )
    suspend fun ativasDaMoto(motoId: String): List<LeituraHodometro>

    @Query("SELECT * FROM leituras_hodometro WHERE id = :id AND excluidoEm IS NULL")
    suspend fun buscar(id: String): LeituraHodometro?

    /** Exclusão lógica (rastro para a sincronização); devolve as linhas afetadas: 1 se estava ativa. */
    @Query(
        "UPDATE leituras_hodometro SET excluidoEm = :quando, alteradoEm = :quando " +
            "WHERE id = :id AND excluidoEm IS NULL",
    )
    suspend fun marcarExcluida(id: String, quando: Long): Int

    /** Quantos registros ativos do diário desta moto têm km: sem leitura, eles perderiam a origem do hodômetro. */
    @Query(
        "SELECT COUNT(*) FROM registros WHERE motoId = :motoId AND hodometroKm IS NOT NULL AND excluidoEm IS NULL",
    )
    suspend fun registrosComKm(motoId: String): Int
}
