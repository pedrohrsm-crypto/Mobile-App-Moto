package com.pedrohrsm.diariodebordo.garagem.dominio

/** Plano grátis: 1 moto (plano de produto). Null quer dizer sem limite, o que só o premium (fase 6) libera. */
const val LIMITE_MOTOS_GRATIS = 1

/** Regra única do limite de motos: a garagem esconde o botão e o cadastro recusa pela mesma função. */
fun podeCadastrarMoto(motosAtivas: Int, limite: Int? = LIMITE_MOTOS_GRATIS): Boolean =
    limite == null || motosAtivas < limite

class LimiteDeMotosException(val limite: Int) : IllegalStateException("O plano grátis permite $limite moto")
