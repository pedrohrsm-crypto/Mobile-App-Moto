package com.pedrohrsm.diariodebordo.plano.dominio

import com.pedrohrsm.diariodebordo.numeros.Leitor

enum class CampoItem { NOME, INTERVALO, KM, DIAS }

/** Campos inválidos do item do plano. Precisa de nome e de pelo menos um intervalo (km ou dias). */
fun validarItem(nome: String, kmTexto: String, diasTexto: String): Set<CampoItem> = buildSet {
    if (nome.isBlank()) add(CampoItem.NOME)
    val km = kmTexto.takeIf { it.isNotBlank() }?.let(Leitor::km)
    val dias = diasTexto.takeIf { it.isNotBlank() }?.trim()?.toIntOrNull()
    if (kmTexto.isNotBlank() && (km == null || km <= 0)) add(CampoItem.KM)
    if (diasTexto.isNotBlank() && (dias == null || dias <= 0 || dias > 3_650)) add(CampoItem.DIAS)
    if (kmTexto.isBlank() && diasTexto.isBlank()) add(CampoItem.INTERVALO)
}
