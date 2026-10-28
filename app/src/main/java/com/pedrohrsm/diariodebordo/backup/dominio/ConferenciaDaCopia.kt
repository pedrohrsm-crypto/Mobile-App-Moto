package com.pedrohrsm.diariodebordo.backup.dominio

import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.dominio.podeCadastrarMoto
import com.pedrohrsm.diariodebordo.rota.data.Rota

/** Nome de foto aceito no arquivo: só nome simples, sem pasta e sem `..` (o arquivo pode vir de qualquer lugar). */
private val NOME_SEGURO = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,99}")

fun nomeDeFotoValido(nome: String): Boolean = NOME_SEGURO.matches(nome) && ".." !in nome

/** A rota excluída nunca guarda o trajeto (risco #23), venha de onde vier. */
fun Rota.semTrajetoSeExcluida(): Rota = if (excluidoEm != null && polilinha.isNotEmpty()) copy(polilinha = "") else this

/**
 * Confere o conteúdo do arquivo antes de qualquer gravação: ids preenchidos e sem repetição, cada filho apontando
 * para um pai do próprio arquivo, nome de foto seguro, coordenada possível e nenhuma rota em gravação.
 * Devolve a cópia normalizada (rota excluída sem traçado; registro cuja rota não veio fica sem a rota).
 */
fun conferir(copia: CopiaDeSeguranca): CopiaDeSeguranca {
    if (copia.formato > FORMATO_ATUAL) throw FormatoNovoDemaisException(copia.formato)
    fun recusar(motivo: String): Nothing = throw ArquivoInvalidoException(motivo)
    fun <T> idsUnicos(tabela: String, itens: List<T>, id: (T) -> String): Set<String> {
        val ids = itens.map(id)
        if (ids.any { it.isBlank() }) recusar("$tabela com id vazio")
        if (ids.size != ids.toSet().size) recusar("$tabela com id repetido")
        return ids.toSet()
    }
    val motos = idsUnicos("motos", copia.motos) { it.id }
    idsUnicos("leituras_hodometro", copia.leituras) { it.id }
    val registros = idsUnicos("registros", copia.registros) { it.id }
    idsUnicos("fotos_registro", copia.fotos) { it.id }
    idsUnicos("itens_plano", copia.itensDoPlano) { it.id }
    val rotas = idsUnicos("rotas", copia.rotas) { it.id }

    val orfao = copia.leituras.any { it.motoId !in motos } ||
        copia.registros.any { it.motoId !in motos } ||
        copia.itensDoPlano.any { it.motoId !in motos } ||
        copia.rotas.any { it.motoId !in motos } ||
        copia.fotos.any { it.registroId !in registros }
    if (orfao) recusar("item que aponta para uma moto ou um registro fora do arquivo")
    if (copia.fotos.any { !nomeDeFotoValido(it.arquivo) }) recusar("nome de foto recusado")
    if (copia.rotas.any { it.emGravacao }) recusar("rota ainda em gravação")
    val coordenadaImpossivel = copia.registros.any { r ->
        r.latitude?.let { it !in -90.0..90.0 } == true || r.longitude?.let { it !in -180.0..180.0 } == true
    }
    if (coordenadaImpossivel) recusar("coordenada fora do globo")

    return copia.copy(
        rotas = copia.rotas.map { it.semTrajetoSeExcluida() },
        registros = copia.registros.map { if (it.rotaId == null || it.rotaId in rotas) it else it.copy(rotaId = null) },
    )
}

/** Moto a escolher (decisão #13): a importação deixaria mais motos ativas do que o plano permite. */
class EscolhaDeMotoNecessariaException(val candidatas: List<Moto>) :
    IllegalStateException("A importação passa do limite de motos: escolha qual fica")

/** O que a importação grava depois do limite de motos: a cópia sem as motos que saem, e as do aparelho a excluir. */
data class CopiaNoLimite(val copia: CopiaDeSeguranca, val motosDoAparelhoAExcluir: List<String>)

/**
 * Decisão #13: importar não deixa mais motos ativas do que o [limite] do plano. Quando passaria, a pessoa escolhe
 * [motoQueFica]: a moto do arquivo que não fica não entra (nem o que é dela), e a do aparelho que não fica é
 * excluída como na garagem. Sem escolha válida, lança [EscolhaDeMotoNecessariaException] com as candidatas.
 * A conta do limite é a de [podeCadastrarMoto]: cabe se daria para cadastrar a última das motos.
 */
fun aplicarLimiteDeMotos(
    copia: CopiaDeSeguranca,
    ativasNoAparelho: List<Moto>,
    limite: Int?,
    motoQueFica: String?,
): CopiaNoLimite {
    val candidatas = (ativasNoAparelho + copia.motos.filter { it.excluidoEm == null }).distinctBy { it.id }
    if (candidatas.isEmpty() || podeCadastrarMoto(candidatas.size - 1, limite)) return CopiaNoLimite(copia, emptyList())
    if (candidatas.none { it.id == motoQueFica }) throw EscolhaDeMotoNecessariaException(candidatas)
    check(limite == 1) { "Escolher uma moto só resolve o limite de 1 moto (decisão #13)" }

    val saemDoArquivo = copia.motos.map { it.id }.filter { it != motoQueFica }.toSet()
    val registrosQueSaem = copia.registros.filter { it.motoId in saemDoArquivo }.map { it.id }.toSet()
    val filtrada = copia.copy(
        motos = copia.motos.filter { it.id !in saemDoArquivo },
        leituras = copia.leituras.filter { it.motoId !in saemDoArquivo },
        registros = copia.registros.filter { it.id !in registrosQueSaem },
        fotos = copia.fotos.filter { it.registroId !in registrosQueSaem },
        itensDoPlano = copia.itensDoPlano.filter { it.motoId !in saemDoArquivo },
        rotas = copia.rotas.filter { it.motoId !in saemDoArquivo },
    )
    return CopiaNoLimite(filtrada, ativasNoAparelho.map { it.id }.filter { it != motoQueFica })
}
