package com.pedrohrsm.diariodebordo.garagem.data

import com.pedrohrsm.diariodebordo.core.database.Transacao
import com.pedrohrsm.diariodebordo.core.premium.Premium
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
    private val premium: Premium,
) {
    suspend fun cadastrar(moto: Moto, kmInicial: Int): Moto = transacao.executar {
        val ativas = motos.observarAtivos().first().size
        val limite = premium.limiteDeMotos
        if (!podeCadastrarMoto(ativas, limite)) throw LimiteDeMotosException(checkNotNull(limite))
        val criada = motos.criar(moto)
        hodometro.registrar(criada.id, kmInicial, criada.criadoEm)
        criada
    }
}
