package com.pedrohrsm.diariodebordo.plano.dominio

import com.pedrohrsm.diariodebordo.garagem.dominio.KM_MAXIMO
import com.pedrohrsm.diariodebordo.numeros.Leitor
import java.time.LocalDate

/**
 * Regra única do "feito" do item (#69). Corrigir uma leitura do hodômetro leva junto os itens feitos naquele km:
 * o item cuja última vez é o km antigo passa ao km novo, na mesma transação da correção.
 */
fun acompanhaCorrecaoDoHodometro(ultimaVezKm: Int?, kmAntigo: Int): Boolean = ultimaVezKm == kmAntigo

/**
 * Campos inválidos ao editar o feito à mão: o km fica entre 0 e o km atual da moto (sem leitura, até o limite do
 * hodômetro) e a data não passa de hoje. O formulário e o [com.pedrohrsm.diariodebordo.plano.data.PlanoRepositorio]
 * passam por aqui.
 */
fun validarFeito(km: Int?, dia: LocalDate, kmAtual: Int?, hoje: LocalDate): Set<CampoItem> = buildSet {
    if (km == null || km !in 0..(kmAtual ?: KM_MAXIMO)) add(CampoItem.FEITO_KM)
    if (dia.isAfter(hoje)) add(CampoItem.FEITO_DATA)
}

/** O mesmo, a partir do texto digitado no formulário ("12.345" ou "12345"). */
fun validarFeito(kmTexto: String, dia: LocalDate, kmAtual: Int?, hoje: LocalDate): Set<CampoItem> =
    validarFeito(Leitor.km(kmTexto), dia, kmAtual, hoje)

class FeitoRecusadoException(val campos: Set<CampoItem>) :
    IllegalArgumentException("Feito do item recusado: $campos")
