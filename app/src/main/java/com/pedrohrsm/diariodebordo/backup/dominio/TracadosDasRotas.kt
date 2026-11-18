package com.pedrohrsm.diariodebordo.backup.dominio

import com.pedrohrsm.diariodebordo.rota.data.Rota
import com.pedrohrsm.diariodebordo.rota.dominio.Polilinha
import com.pedrohrsm.diariodebordo.rota.dominio.ProtecaoDasRotas
import com.pedrohrsm.diariodebordo.rota.dominio.tracadoDaRota
import com.pedrohrsm.diariodebordo.rota.dominio.visiveis

/**
 * #78 (decisões do dono em 4-Nov-2026): o traçado que sai do aparelho perde as pontas e o que passa dentro das zonas
 * de privacidade ([tracadoDaRota]). A distância e a duração continuam as reais. Rota curta demais, que fica
 * inteira oculta, sai sem traçado. Com a proteção desligada e sem zonas, a rota sai como está. É a única regra do
 * que muda numa rota que sai do aparelho; vale para o backup e para todo compartilhamento futuro.
 */
fun Rota.paraOArquivo(protecao: ProtecaoDasRotas, segredo: ByteArray): Rota {
    if (!protecao.ativa || polilinha.isEmpty()) return this
    return copy(polilinha = Polilinha.codificarTrechos(protecao.tracadoDaRota(id, polilinha, segredo).visiveis()))
}

/**
 * Mesma regra da mesclagem dos locais (#73): um arquivo com os traçados protegidos ([tracadosProtegidos]) não apaga
 * o traçado completo que o aparelho já tem. Cada rota que chega fica com a polilinha da rota de mesmo id no
 * aparelho; a que o aparelho não tem chega como veio (é tudo o que existe dela). Arquivo sem proteção chega igual.
 */
fun List<Rota>.paraOAparelho(tracadosProtegidos: Boolean, doAparelho: List<Rota>): List<Rota> {
    if (!tracadosProtegidos) return this
    val porId = doAparelho.associateBy { it.id }
    return map { vinda -> porId[vinda.id]?.let { vinda.copy(polilinha = it.polilinha) } ?: vinda }
}
