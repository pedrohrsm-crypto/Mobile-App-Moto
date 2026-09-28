package com.pedrohrsm.diariodebordo.backup.data

import com.pedrohrsm.diariodebordo.diario.data.FotoRegistro
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.rota.data.Rota

/** Tabelas em memória com a semântica do @Upsert: insere o id novo, substitui o existente. */
class FakeBackupDao : BackupDao {
    var motos = listOf<Moto>()
    var leituras = listOf<LeituraHodometro>()
    var registros = listOf<Registro>()
    var fotos = listOf<FotoRegistro>()
    var itens = listOf<ItemDoPlano>()
    var rotas = listOf<Rota>()

    /** Quando preenchida, a gravação das fotos falha (para testar a volta atrás). */
    var falhaAoGravarFotos: Exception? = null

    private fun <T> upsert(atuais: List<T>, novos: List<T>, id: (T) -> String): List<T> {
        val ids = novos.map(id).toSet()
        return atuais.filter { id(it) !in ids } + novos
    }

    override suspend fun motos() = motos
    override suspend fun leituras() = leituras
    override suspend fun registros() = registros
    override suspend fun fotos() = fotos
    override suspend fun itensDoPlano() = itens
    override suspend fun rotas() = rotas.filter { !it.emGravacao }

    override suspend fun gravarMotos(itens: List<Moto>) {
        motos = upsert(motos, itens) { it.id }
    }

    override suspend fun gravarLeituras(itens: List<LeituraHodometro>) {
        leituras = upsert(leituras, itens) { it.id }
    }

    override suspend fun gravarRegistros(itens: List<Registro>) {
        registros = upsert(registros, itens) { it.id }
    }

    override suspend fun gravarFotos(itens: List<FotoRegistro>) {
        falhaAoGravarFotos?.let { throw it }
        fotos = upsert(fotos, itens) { it.id }
    }

    override suspend fun gravarItensDoPlano(itens: List<ItemDoPlano>) {
        this.itens = upsert(this.itens, itens) { it.id }
    }

    override suspend fun gravarRotas(itens: List<Rota>) {
        rotas = upsert(rotas, itens) { it.id }
    }
}
