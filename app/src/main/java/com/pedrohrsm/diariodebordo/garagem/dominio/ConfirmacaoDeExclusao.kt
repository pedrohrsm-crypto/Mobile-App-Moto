package com.pedrohrsm.diariodebordo.garagem.dominio

/** O que some junto com a moto, contado antes de excluir para o diálogo dizer a verdade. */
data class ContagemDaMoto(val registros: Int, val fotos: Int, val itensDoPlano: Int, val rotas: Int, val leituras: Int)

/**
 * Regra única da confirmação: o botão de excluir só libera quando o texto digitado é o apelido da moto.
 * Espaço nas pontas e maiúsculas não contam (o teclado do celular capitaliza sozinho); o resto, sim.
 */
fun confirmaExclusao(digitado: String, apelido: String): Boolean =
    apelido.isNotBlank() && digitado.trim().equals(apelido.trim(), ignoreCase = true)

/** Uma rota desta moto está gravando: excluir agora deixaria o serviço gravando numa rota que sumiu. */
class GravacaoEmAndamentoException : IllegalStateException("Há uma rota desta moto em gravação")
