package com.pedrohrsm.diariodebordo.plano.dominio

import com.pedrohrsm.diariodebordo.diario.data.TipoOcorrencia
import com.pedrohrsm.diariodebordo.garagem.data.CategoriaMoto

/** Item sugerido de um plano-base. Os números são ponto de partida: a pessoa ajusta pelo manual da moto. */
data class ItemBase(val nome: String, val km: Int?, val dias: Int?)

/**
 * Planos-base por categoria (plano de ação 1.3). Valores genéricos de mercado, marcados na tela como
 * "dado do usuário": o manual da moto manda. Sem categoria, vale o plano street.
 */
fun planoBase(categoria: CategoriaMoto?): List<ItemBase> = when (categoria ?: CategoriaMoto.STREET) {
    CategoriaMoto.STREET -> listOf(
        ItemBase(NOME_TROCA_DE_OLEO, km = 5_000, dias = 180),
        ItemBase("Filtro de óleo", km = 10_000, dias = 365),
        ItemBase("Filtro de ar", km = 10_000, dias = 365),
        ItemBase("Lubrificar a relação", km = 1_000, dias = 30),
        ItemBase("Trocar a relação", km = 20_000, dias = null),
        ItemBase("Velas", km = 10_000, dias = 730),
        ItemBase("Pastilhas de freio (inspeção)", km = 5_000, dias = 180),
        ItemBase("Fluido de freio", km = null, dias = 730),
        ItemBase("Pneus (inspeção)", km = 3_000, dias = 90),
    )
    CategoriaMoto.CUSTOM -> listOf(
        ItemBase(NOME_TROCA_DE_OLEO, km = 8_000, dias = 365),
        ItemBase("Filtro de óleo", km = 8_000, dias = 365),
        ItemBase("Filtro de ar", km = 16_000, dias = 730),
        ItemBase("Correia ou relação (inspeção)", km = 8_000, dias = 365),
        ItemBase("Velas", km = 16_000, dias = 730),
        ItemBase("Pastilhas de freio (inspeção)", km = 8_000, dias = 365),
        ItemBase("Fluido de freio", km = null, dias = 730),
        ItemBase("Pneus (inspeção)", km = 3_000, dias = 90),
        ItemBase("Bateria (inspeção)", km = null, dias = 365),
    )
    CategoriaMoto.TRAIL -> listOf(
        ItemBase(NOME_TROCA_DE_OLEO, km = 4_000, dias = 180),
        ItemBase("Filtro de óleo", km = 8_000, dias = 365),
        ItemBase("Limpar o filtro de ar", km = 2_000, dias = 90),
        ItemBase("Lubrificar a relação", km = 500, dias = 15),
        ItemBase("Trocar a relação", km = 15_000, dias = null),
        ItemBase("Velas", km = 8_000, dias = 365),
        ItemBase("Pastilhas de freio (inspeção)", km = 4_000, dias = 180),
        ItemBase("Fluido de freio", km = null, dias = 730),
        ItemBase("Rolamentos e suspensão (inspeção)", km = 10_000, dias = 365),
    )
    CategoriaMoto.SCOOTER -> listOf(
        ItemBase(NOME_TROCA_DE_OLEO, km = 3_000, dias = 180),
        ItemBase("Óleo da transmissão", km = 6_000, dias = 365),
        ItemBase("Filtro de ar", km = 6_000, dias = 365),
        ItemBase("Correia do CVT", km = 20_000, dias = 730),
        ItemBase("Velas", km = 8_000, dias = 730),
        ItemBase("Pastilhas de freio (inspeção)", km = 4_000, dias = 180),
        ItemBase("Fluido de freio", km = null, dias = 730),
        ItemBase("Pneus (inspeção)", km = 3_000, dias = 90),
    )
}

/** Itens de inspeção que uma ocorrência cria no plano (plano de ação 1.5). */
fun inspecoesDaOcorrencia(tipo: TipoOcorrencia?): List<String> = when (tipo) {
    TipoOcorrencia.QUEDA -> listOf(
        "Conferir manetes, pedaleiras e retrovisores",
        "Conferir guidão e alinhamento da roda dianteira",
        "Conferir carenagem, piscas e faróis",
        "Conferir vazamentos embaixo da moto",
    )
    TipoOcorrencia.BARULHO -> listOf(
        "Conferir folga e lubrificação da relação",
        "Conferir escapamento e fixações",
        "Conferir rolamentos das rodas",
    )
    TipoOcorrencia.VAZAMENTO -> listOf(
        "Conferir o nível do óleo do motor",
        "Conferir o fluido de freio",
        "Achar a origem do vazamento",
    )
    TipoOcorrencia.OUTRA, null -> listOf("Inspecionar a moto")
}
