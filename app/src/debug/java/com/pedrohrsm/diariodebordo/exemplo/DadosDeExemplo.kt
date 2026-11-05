package com.pedrohrsm.diariodebordo.exemplo

import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.CategoriaMoto
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.localizacao.Ponto
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.plano.data.OrigemItem
import com.pedrohrsm.diariodebordo.plano.dominio.CHAVE_TROCA_DE_OLEO
import com.pedrohrsm.diariodebordo.plano.dominio.NOME_TROCA_DE_OLEO
import com.pedrohrsm.diariodebordo.rota.data.Rota
import com.pedrohrsm.diariodebordo.rota.dominio.Polilinha
import com.pedrohrsm.diariodebordo.rota.dominio.distanciaTotalM
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.math.PI
import kotlin.math.sin

/**
 * Dados de exemplo para capturas e vídeo do estudo de caso (#77). Existe só no build debug (source set `debug`):
 * o release nem compila este arquivo. Tudo é inventado: motos sem relação com nenhuma pessoa, valores redondos,
 * rotas desenhadas por fórmula (nunca gravadas por GPS) numa estrada pública conhecida.
 * Os ids são derivados de um nome fixo, então carregar duas vezes regrava as mesmas linhas, sem duplicar.
 */
data class DadosDeExemplo(
    val motos: List<Moto>,
    val leituras: List<LeituraHodometro>,
    val registros: List<Registro>,
    val itensDoPlano: List<ItemDoPlano>,
    val rotas: List<Rota>,
)

/** Retângulo em graus que contém todo o traçado de exemplo. */
data class CaixaDaEstrada(val latMin: Double, val latMax: Double, val lonMin: Double, val lonMax: Double) {
    fun contem(p: Ponto): Boolean = p.latitude in latMin..latMax && p.longitude in lonMin..lonMax
}

/**
 * Trecho da serra do Rio do Rastro (SC-390), referência de cerca de 28,39° S 49,55° O. Mata e pasto, sem endereço
 * residencial no traçado. A caixa é larga de propósito: o traçado é fórmula, não a geometria real da pista.
 */
val CAIXA_RIO_DO_RASTRO = CaixaDaEstrada(latMin = -28.42, latMax = -28.37, lonMin = -49.58, lonMax = -49.53)

const val MOTOS_DE_EXEMPLO = 2
const val ROTAS_DE_EXEMPLO = 2
const val MESES_DE_EXEMPLO = 6

private const val DIA_MS = 86_400_000L

private fun idFixo(nome: String): String = UUID.nameUUIDFromBytes("diario-exemplo:$nome".toByteArray()).toString()

/**
 * Traçado em zigue-zague (as curvas da serra) subindo de [inicio] a [fim], com [pontos] pontos, sem sorteio:
 * o mesmo pedido gera sempre o mesmo traçado. [fase] muda onde as curvas começam.
 */
fun trajetoSintetico(
    inicio: Pair<Double, Double>,
    fim: Pair<Double, Double>,
    pontos: Int,
    fase: Double,
    instanteInicial: Long,
    passoMs: Long,
): List<Ponto> {
    require(pontos >= 2) { "Traçado precisa de ao menos 2 pontos" }
    return (0 until pontos).map { i ->
        val t = i.toDouble() / (pontos - 1)
        val curva = sin(t * 14 * PI + fase) * AMPLITUDE_CURVA_GRAUS * sin(t * PI)
        Ponto(
            latitude = inicio.first + (fim.first - inicio.first) * t + curva,
            longitude = inicio.second + (fim.second - inicio.second) * t - curva,
            instante = instanteInicial + passoMs * i,
            precisaoM = 5f,
        )
    }
}

private const val AMPLITUDE_CURVA_GRAUS = 0.0018

private val BASE_DA_SERRA = -28.410 to -49.565
private val TOPO_DA_SERRA = -28.385 to -49.540

/** Monta o conjunto de exemplo em relação a [agora]: os estados do plano (em dia, próximo, vencido) valem hoje. */
fun gerarDadosDeExemplo(agora: Instant, fuso: ZoneId): DadosDeExemplo {
    val agoraMs = agora.toEpochMilli()
    fun diasAtras(dias: Int) = agoraMs - dias * DIA_MS
    val trilha = idFixo("moto-trilha")
    val urbana = idFixo("moto-urbana")

    fun moto(id: String, apelido: String, modelo: String, ano: Int, categoria: CategoriaMoto) = Moto(
        id = id,
        apelido = apelido,
        marca = "Marca Exemplo",
        modelo = modelo,
        ano = ano,
        categoria = categoria,
        criadoEm = diasAtras(MESES_DE_EXEMPLO * 30),
        alteradoEm = agoraMs,
        fusoHorario = fuso.id,
    )

    val motos = listOf(
        moto(trilha, "Trilheira (exemplo)", "Trail 300", 2022, CategoriaMoto.TRAIL),
        moto(urbana, "Urbana (exemplo)", "Street 160", 2020, CategoriaMoto.STREET),
    )

    // Leituras mensais, estritamente crescentes (regra do hodômetro: o km nunca volta nem repete).
    val kmTrilha = listOf(12_000, 12_850, 13_700, 14_900, 15_600, 16_400, 17_300)
    val kmUrbana = listOf(31_000, 31_600, 32_150, 32_900, 33_400, 34_000, 34_500)
    fun leituras(motoId: String, kms: List<Int>) = kms.mapIndexed { i, km ->
        val quando = diasAtras((kms.size - 1 - i) * 30)
        LeituraHodometro(
            id = idFixo("leitura-$motoId-$i"),
            motoId = motoId,
            km = km,
            lidaEm = quando,
            criadoEm = quando,
            alteradoEm = quando,
            fusoHorario = fuso.id,
        )
    }
    val leituras = leituras(trilha, kmTrilha) + leituras(urbana, kmUrbana)

    fun registro(
        nome: String,
        motoId: String,
        tipo: TipoRegistro,
        titulo: String,
        dias: Int,
        km: Int?,
        descricao: String = "",
        valorCentavos: Long? = null,
        litrosMl: Long? = null,
        rotaId: String? = null,
    ) = Registro(
        id = idFixo("registro-$nome"),
        motoId = motoId,
        tipo = tipo,
        titulo = titulo,
        descricao = descricao,
        dataEm = diasAtras(dias),
        hodometroKm = km,
        valorCentavos = valorCentavos,
        litrosMl = litrosMl,
        tanqueCheio = litrosMl != null,
        rotaId = rotaId,
        criadoEm = diasAtras(dias),
        alteradoEm = diasAtras(dias),
        fusoHorario = fuso.id,
    )

    val rotaSubida = idFixo("rota-subida")
    val rotaIdaEVolta = idFixo("rota-ida-e-volta")

    val registros = listOf(
        registro(
            "t1",
            trilha,
            TipoRegistro.ABASTECIMENTO,
            "Abastecimento",
            150,
            12_850,
            valorCentavos = 18_000,
            litrosMl = 3_000,
        ),
        registro(
            "t2",
            trilha,
            TipoRegistro.MANUTENCAO,
            "Troca de óleo",
            120,
            13_700,
            "Óleo sintético, filtro novo.",
            21_000,
        ),
        registro(
            "t3",
            trilha,
            TipoRegistro.ABASTECIMENTO,
            "Abastecimento",
            90,
            14_900,
            valorCentavos = 19_500,
            litrosMl = 3_200,
        ),
        registro(
            "t4",
            trilha,
            TipoRegistro.NOTA,
            "Ajuste da corrente",
            60,
            15_600,
            "Folga conferida e corrente lubrificada.",
        ),
        registro(
            "t5",
            trilha,
            TipoRegistro.ABASTECIMENTO,
            "Abastecimento",
            30,
            16_400,
            valorCentavos = 20_400,
            litrosMl = 3_100,
        ),
        registro(
            "t6",
            trilha,
            TipoRegistro.ROLE,
            "Subida da serra",
            21,
            null,
            "Dia limpo, estrada vazia.",
            rotaId = rotaSubida,
        ),
        registro(
            "t7",
            trilha,
            TipoRegistro.ROLE,
            "Ida e volta no mirante",
            7,
            null,
            "Parada rápida no topo.",
            rotaId = rotaIdaEVolta,
        ),
        registro(
            "t8",
            trilha,
            TipoRegistro.ABASTECIMENTO,
            "Abastecimento",
            0,
            17_300,
            valorCentavos = 21_000,
            litrosMl = 3_300,
        ),
        registro(
            "u1",
            urbana,
            TipoRegistro.ABASTECIMENTO,
            "Abastecimento",
            150,
            31_600,
            valorCentavos = 9_000,
            litrosMl = 1_800,
        ),
        registro(
            "u2",
            urbana,
            TipoRegistro.MANUTENCAO,
            "Pastilhas de freio",
            120,
            32_150,
            "Troca das pastilhas dianteiras.",
            12_000,
        ),
        registro(
            "u3",
            urbana,
            TipoRegistro.NOTA,
            "Pneu traseiro",
            90,
            32_900,
            "Ainda com sulco, rever em 3 meses.",
        ),
        registro(
            "u4",
            urbana,
            TipoRegistro.ABASTECIMENTO,
            "Abastecimento",
            0,
            34_500,
            valorCentavos = 9_600,
            litrosMl = 1_900,
        ),
    )

    fun item(
        nome: String,
        motoId: String,
        titulo: String,
        intervaloKm: Int?,
        intervaloDias: Int?,
        ultimaVezKm: Int,
        diasDesdeAUltimaVez: Int,
        chave: String? = null,
    ) = ItemDoPlano(
        id = idFixo("item-$nome"),
        motoId = motoId,
        chave = chave,
        nome = titulo,
        intervaloKm = intervaloKm,
        intervaloDias = intervaloDias,
        ultimaVezKm = ultimaVezKm,
        ultimaVezEm = diasAtras(diasDesdeAUltimaVez),
        feitoEm = diasAtras(diasDesdeAUltimaVez),
        origem = OrigemItem.USUARIO,
        criadoEm = diasAtras(MESES_DE_EXEMPLO * 30),
        alteradoEm = agoraMs,
        fusoHorario = fuso.id,
    )

    val itens = listOf(
        // Trilheira, km atual 17.300: óleo a 400 km do limite (próximo), filtro em dia, pneus passados (vencido).
        item("t-oleo", trilha, NOME_TROCA_DE_OLEO, 5_000, 180, 12_700, 40, CHAVE_TROCA_DE_OLEO),
        item("t-filtro", trilha, "Filtro de ar", 10_000, 365, 15_300, 90),
        item("t-pneus", trilha, "Pneus (inspeção)", 3_000, 90, 13_900, 120),
        item("t-relacao", trilha, "Lubrificar a relação", 1_000, 30, 17_000, 10),
        // Urbana, km atual 34.500: óleo em dia, pastilhas a 10 dias do prazo (próximo), fluido vencido pelo tempo.
        item("u-oleo", urbana, NOME_TROCA_DE_OLEO, 3_000, 180, 33_400, 30, CHAVE_TROCA_DE_OLEO),
        item("u-pastilhas", urbana, "Pastilhas de freio (inspeção)", 5_000, 180, 32_150, 170),
        item("u-fluido", urbana, "Fluido de freio", null, 730, 28_000, 800),
    )

    // Duas rotas na mesma estrada: a subida e uma ida e volta até o topo (a volta refaz o trecho com outra fase).
    val subida = trajetoSintetico(BASE_DA_SERRA, TOPO_DA_SERRA, 90, 0.0, diasAtras(21), 15_000)
    val ida = trajetoSintetico(BASE_DA_SERRA, TOPO_DA_SERRA, 70, 1.0, diasAtras(7), 15_000)
    val volta = trajetoSintetico(TOPO_DA_SERRA, BASE_DA_SERRA, 70, 2.0, ida.last().instante + 600_000, 12_000)
    fun rota(id: String, motoId: String, pontos: List<Ponto>) = Rota(
        id = id,
        motoId = motoId,
        iniciadaEm = pontos.first().instante,
        terminadaEm = pontos.last().instante,
        distanciaM = distanciaTotalM(pontos).toLong(),
        duracaoS = (pontos.last().instante - pontos.first().instante) / 1_000,
        polilinha = Polilinha.codificar(pontos),
        emGravacao = false,
        criadoEm = pontos.last().instante,
        alteradoEm = pontos.last().instante,
        fusoHorario = fuso.id,
    )
    val rotas = listOf(rota(rotaSubida, trilha, subida), rota(rotaIdaEVolta, trilha, ida + volta))

    return DadosDeExemplo(motos, leituras, registros, itens, rotas)
}
