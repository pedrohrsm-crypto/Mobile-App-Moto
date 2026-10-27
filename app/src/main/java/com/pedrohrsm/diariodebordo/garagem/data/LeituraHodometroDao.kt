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

    @Query("SELECT MAX(km) FROM leituras_hodometro WHERE motoId = :motoId AND excluidoEm IS NULL")
    suspend fun kmAtual(motoId: String): Int?

    @Query("SELECT motoId, MAX(km) AS km FROM leituras_hodometro WHERE excluidoEm IS NULL GROUP BY motoId")
    fun observarKmAtuais(): Flow<List<KmAtual>>

    @Insert
    suspend fun inserir(item: LeituraHodometro)
}
