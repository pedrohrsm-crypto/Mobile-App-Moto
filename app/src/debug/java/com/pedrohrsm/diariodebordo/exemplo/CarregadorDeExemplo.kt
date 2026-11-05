package com.pedrohrsm.diariodebordo.exemplo

import com.pedrohrsm.diariodebordo.backup.data.BackupDao
import com.pedrohrsm.diariodebordo.core.database.Transacao
import java.time.Clock
import javax.inject.Inject

/**
 * Grava os dados de exemplo pelas escritas em bloco do [BackupDao] (upsert), tudo ou nada. Como os ids são fixos,
 * carregar de novo regrava as mesmas linhas. Não apaga nada que já exista.
 */
class CarregadorDeExemplo @Inject constructor(
    private val dao: BackupDao,
    private val transacao: Transacao,
    private val relogio: Clock,
) {
    suspend fun carregar(): DadosDeExemplo {
        val dados = gerarDadosDeExemplo(relogio.instant(), relogio.zone)
        transacao.executar {
            dao.gravarMotos(dados.motos)
            dao.gravarLeituras(dados.leituras)
            dao.gravarRegistros(dados.registros)
            dao.gravarItensDoPlano(dados.itensDoPlano)
            dao.gravarRotas(dados.rotas)
        }
        return dados
    }
}
