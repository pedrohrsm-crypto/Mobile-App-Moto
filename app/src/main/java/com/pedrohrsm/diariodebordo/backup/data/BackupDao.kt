package com.pedrohrsm.diariodebordo.backup.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistro
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.rota.data.Rota

/**
 * Leitura e escrita em bloco, só para exportar e importar. As telas continuam usando os DAOs de cada
 * funcionalidade. "Todos" inclui os excluídos, para a mesclagem comparar com o que já existe. A rota em
 * gravação fica de fora dos dois lados, e `pontos_rota` não tem consulta aqui: os pontos brutos não saem.
 */
@Dao
interface BackupDao {
    @Query("SELECT * FROM motos")
    suspend fun motos(): List<Moto>

    @Query("SELECT * FROM leituras_hodometro")
    suspend fun leituras(): List<LeituraHodometro>

    @Query("SELECT * FROM registros")
    suspend fun registros(): List<Registro>

    @Query("SELECT * FROM fotos_registro")
    suspend fun fotos(): List<FotoRegistro>

    @Query("SELECT * FROM itens_plano")
    suspend fun itensDoPlano(): List<ItemDoPlano>

    @Query("SELECT * FROM rotas WHERE emGravacao = 0")
    suspend fun rotas(): List<Rota>

    @Upsert
    suspend fun gravarMotos(itens: List<Moto>)

    @Upsert
    suspend fun gravarLeituras(itens: List<LeituraHodometro>)

    @Upsert
    suspend fun gravarRegistros(itens: List<Registro>)

    @Upsert
    suspend fun gravarFotos(itens: List<FotoRegistro>)

    @Upsert
    suspend fun gravarItensDoPlano(itens: List<ItemDoPlano>)

    @Upsert
    suspend fun gravarRotas(itens: List<Rota>)
}
