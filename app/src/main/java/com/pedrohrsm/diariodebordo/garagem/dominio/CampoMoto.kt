package com.pedrohrsm.diariodebordo.garagem.dominio

import com.pedrohrsm.diariodebordo.numeros.Leitor

enum class CampoMoto { APELIDO, MARCA, MODELO, ANO, KM }

/**
 * Campos inválidos do cadastro de moto. Vazio quando pode salvar.
 * [kmTexto] é null na edição (o km muda pelo hodômetro, não pelo cadastro).
 */
fun validarMoto(
    apelido: String,
    marca: String,
    modelo: String,
    anoTexto: String,
    kmTexto: String?,
    anoAtual: Int,
): Set<CampoMoto> = buildSet {
    if (apelido.isBlank()) add(CampoMoto.APELIDO)
    if (marca.isBlank()) add(CampoMoto.MARCA)
    if (modelo.isBlank()) add(CampoMoto.MODELO)
    if (anoTexto.isNotBlank()) {
        val ano = anoTexto.trim().toIntOrNull()
        if (ano == null || ano < 1900 || ano > anoAtual + 1) add(CampoMoto.ANO)
    }
    if (kmTexto != null) {
        val km = Leitor.km(kmTexto)
        if (km == null || validarLeitura(null, km) != ValidacaoLeitura.Aceita) add(CampoMoto.KM)
    }
}
