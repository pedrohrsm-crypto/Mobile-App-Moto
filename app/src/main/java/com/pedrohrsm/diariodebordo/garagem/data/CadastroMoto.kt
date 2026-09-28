package com.pedrohrsm.diariodebordo.garagem.data

import com.pedrohrsm.diariodebordo.core.database.Transacao
import com.pedrohrsm.diariodebordo.core.premium.LimiteDeMotosException
import com.pedrohrsm.diariodebordo.core.premium.Premium
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Cadastro completo de uma moto: confere o limite do plano, cria a moto e a primeira leitura
 * do hodômetro numa transação só.
 */
class CadastroMoto @Inject constructor(
    private val motos: MotoRepositorio,
    private val hodometro: HodometroRepositorio,
    private val premium: Premium,
    private val transacao: Transacao,
) {
    suspend fun cadastrar(moto: Moto, kmInicial: Int): Moto = transacao.executar {
        val ativas = motos.observarAtivos().first().size
        if (!premium.podeCadastrarMoto(ativas)) throw LimiteDeMotosException(premium.limiteDeMotos ?: 0)
        val criada = motos.criar(moto)
        hodometro.registrar(criada.id, kmInicial, criada.criadoEm)
        criada
    }
}
