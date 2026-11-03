package com.pedrohrsm.diariodebordo.garagem.dominio

/** Regra única do hodômetro: o km nunca volta. Sem Android aqui, para ser testada na JVM. */
sealed interface ValidacaoLeitura {
    data object Aceita : ValidacaoLeitura
    data object Negativa : ValidacaoLeitura
    data object AcimaDoLimite : ValidacaoLeitura
    data class MenorQueAnterior(val anteriorKm: Int) : ValidacaoLeitura
    data class IgualAnterior(val anteriorKm: Int) : ValidacaoLeitura
}

const val KM_MAXIMO = 9_999_999

fun validarLeitura(anteriorKm: Int?, novoKm: Int): ValidacaoLeitura = when {
    novoKm < 0 -> ValidacaoLeitura.Negativa
    novoKm > KM_MAXIMO -> ValidacaoLeitura.AcimaDoLimite
    anteriorKm == null -> ValidacaoLeitura.Aceita
    novoKm < anteriorKm -> ValidacaoLeitura.MenorQueAnterior(anteriorKm)
    novoKm == anteriorKm -> ValidacaoLeitura.IgualAnterior(anteriorKm)
    else -> ValidacaoLeitura.Aceita
}

class LeituraRecusadaException(val motivo: ValidacaoLeitura) :
    IllegalArgumentException("Leitura de hodômetro recusada: $motivo")

/** Uma leitura vista pela regra: só o que ela precisa (sem Room), para ser testada na JVM. */
data class PontoDoHodometro(val id: String, val km: Int, val lidaEm: Long, val fusoHorario: String = "")

/** Qual vizinha (pela data da leitura) o km editado violou. */
enum class Vizinha { ANTERIOR, SEGUINTE }

/**
 * Resultado de editar uma leitura já lançada. O km editado fica estritamente entre a vizinha anterior e a
 * seguinte; [ForaDoIntervalo] devolve as duas vizinhas (para a mensagem "entre X e Y") e qual delas foi violada.
 */
sealed interface ValidacaoEdicao {
    data object Aceita : ValidacaoEdicao
    data object Negativa : ValidacaoEdicao
    data object AcimaDoLimite : ValidacaoEdicao
    data object NaoEncontrada : ValidacaoEdicao

    /**
     * Dentro do intervalo das vizinhas, mas igual ao km de outra leitura ativa da moto. Só acontece quando a
     * ordem pela data não é crescente no km (leituras retroativas do diário): o km de cada leitura é único.
     */
    data class IgualAOutraLeitura(val outra: PontoDoHodometro) : ValidacaoEdicao
    data class ForaDoIntervalo(
        val violada: Vizinha,
        val anterior: PontoDoHodometro?,
        val seguinte: PontoDoHodometro?,
    ) : ValidacaoEdicao
}

/** As leituras ativas na ordem da data; empate de data desempata pelo km e, por último, pelo id. */
fun ordenarPelaData(leituras: List<PontoDoHodometro>): List<PontoDoHodometro> =
    leituras.sortedWith(compareBy<PontoDoHodometro> { it.lidaEm }.thenBy { it.km }.thenBy { it.id })

/**
 * Valida a edição da leitura [id] para [novoKm] contra as vizinhas pela data da leitura. Sem vizinha anterior
 * (primeira leitura) só vale o limite de cima, e vice-versa. A própria leitura não conta como vizinha.
 * Além das vizinhas, o km não pode repetir o de nenhuma outra leitura ativa ([ValidacaoEdicao.IgualAOutraLeitura]).
 */
fun validarEdicao(leituras: List<PontoDoHodometro>, id: String, novoKm: Int): ValidacaoEdicao {
    val ordenadas = ordenarPelaData(leituras)
    val posicao = ordenadas.indexOfFirst { it.id == id }
    return when {
        posicao < 0 -> ValidacaoEdicao.NaoEncontrada
        novoKm < 0 -> ValidacaoEdicao.Negativa
        novoKm > KM_MAXIMO -> ValidacaoEdicao.AcimaDoLimite
        else -> {
            val anterior = ordenadas.getOrNull(posicao - 1)
            val seguinte = ordenadas.getOrNull(posicao + 1)
            when {
                anterior != null && novoKm <= anterior.km ->
                    ValidacaoEdicao.ForaDoIntervalo(Vizinha.ANTERIOR, anterior, seguinte)
                seguinte != null && novoKm >= seguinte.km ->
                    ValidacaoEdicao.ForaDoIntervalo(Vizinha.SEGUINTE, anterior, seguinte)
                else -> ordenadas.firstOrNull { it.id != id && it.km == novoKm }
                    ?.let { ValidacaoEdicao.IgualAOutraLeitura(it) }
                    ?: ValidacaoEdicao.Aceita
            }
        }
    }
}

sealed interface ValidacaoExclusao {
    data object Permitida : ValidacaoExclusao
    data object NaoEncontrada : ValidacaoExclusao

    /** A moto tem registros do diário com km e esta é a única leitura: apagar deixaria o hodômetro sem origem. */
    data object UnicaComRegistrosDeKm : ValidacaoExclusao
}

/**
 * Apagar uma leitura é sempre permitido, menos a única leitura de uma moto que tem registros do diário com km.
 * As vizinhas da apagada continuam válidas entre si: a sequência já era crescente e tirar um elo não a quebra.
 */
fun validarExclusao(leituras: List<PontoDoHodometro>, id: String, motoTemRegistrosComKm: Boolean): ValidacaoExclusao =
    when {
        leituras.none { it.id == id } -> ValidacaoExclusao.NaoEncontrada
        leituras.size == 1 && motoTemRegistrosComKm -> ValidacaoExclusao.UnicaComRegistrosDeKm
        else -> ValidacaoExclusao.Permitida
    }

class EdicaoRecusadaException(val motivo: ValidacaoEdicao) :
    IllegalArgumentException("Edição de leitura de hodômetro recusada: $motivo")

class ExclusaoRecusadaException(val motivo: ValidacaoExclusao) :
    IllegalStateException("Exclusão de leitura de hodômetro recusada: $motivo")
