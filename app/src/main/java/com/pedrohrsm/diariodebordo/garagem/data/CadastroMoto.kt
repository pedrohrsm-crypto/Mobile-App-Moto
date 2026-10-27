package com.pedrohrsm.diariodebordo.garagem.data

import com.pedrohrsm.diariodebordo.core.database.Transacao
import com.pedrohrsm.diariodebordo.garagem.dominio.LIMITE_MOTOS_GRATIS
import com.pedrohrsm.diariodebordo.garagem.dominio.LimiteDeMotosException
import com.pedrohrsm.diariodebordo.garagem.dominio.podeCadastrarMoto
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Cadastro completo de uma moto: confere o limite do plano grátis, cria a moto e a primeira leitura
 * do hodômetro numa transação só. O plano-base de manutenção entra aqui com a frente do plano.
 */
class CadastroMoto @Inject constructor(
    private val motos: MotoRepositorio,
    private val hodometro: HodometroRepositorio,
    private val transacao: Transacao,
) {
    suspend fun cadastrar(moto: Moto, kmInicial: Int): Moto = transacao.executar {
        val ativas = motos.observarAtivos().first().size
        if (!podeCadastrarMoto(ativas)) throw LimiteDeMotosException(LIMITE_MOTOS_GRATIS)
        val criada = motos.criar(moto)
        hodometro.registrar(criada.id, kmInicial, criada.criadoEm)
        criada
    }
}
