package com.pedrohrsm.diariodebordo.backup.dominio

import com.pedrohrsm.diariodebordo.rota.data.Rota
import com.pedrohrsm.diariodebordo.rota.dominio.Polilinha
import com.pedrohrsm.diariodebordo.rota.dominio.ProtecaoDasRotas
import com.pedrohrsm.diariodebordo.rota.dominio.paraOTracadoJaCortado
import com.pedrohrsm.diariodebordo.rota.dominio.tracadoDaRota
import com.pedrohrsm.diariodebordo.rota.dominio.visiveis

/**
 * #78 (decisões do dono em 4-Nov-2026): o traçado que sai do aparelho perde as pontas e o que passa dentro das zonas
 * de privacidade ([tracadoDaRota]). A distância e a duração continuam as reais. Rota curta demais, que fica
 * inteira oculta, sai sem traçado. Com a proteção desligada e sem zonas, a rota sai como está. É a única regra do
 * que muda numa rota que sai do aparelho; vale para o backup e para todo compartilhamento futuro.
 *
 * #88: a rota que chegou já sem as pontas ([Rota.pontasJaCortadasM]) não perde as pontas de novo, se o corte de
 * agora não for maior que o que ela já sofreu ([paraOTracadoJaCortado]); as zonas valem sempre.
 */
fun Rota.paraOArquivo(protecao: ProtecaoDasRotas, segredo: ByteArray): Rota {
    val daRota = protecao.paraOTracadoJaCortado(pontasJaCortadasM)
    if (!daRota.ativa || polilinha.isEmpty()) return this
    return copy(polilinha = Polilinha.codificarTrechos(daRota.tracadoDaRota(id, polilinha, segredo).visiveis()))
}

/**
 * Mesma regra da mesclagem dos locais (#73): um arquivo com os traçados protegidos ([tracadosProtegidos]) não apaga
 * o traçado completo que o aparelho já tem. Cada rota que chega fica com a polilinha da rota de mesmo id no
 * aparelho, e com a marca dela (#88); a que o aparelho não tem chega como veio (é tudo o que existe dela), marcada
 * com o corte que o arquivo diz ter aplicado nas pontas ([pontasCortadasM]). Arquivo sem proteção chega igual e
 * sem marca: a marca nunca vem da rota do arquivo, só do cabeçalho, e só nasce aqui.
 */
fun List<Rota>.paraOAparelho(tracadosProtegidos: Boolean, pontasCortadasM: Int, doAparelho: List<Rota>): List<Rota> {
    if (!tracadosProtegidos) return this
    val porId = doAparelho.associateBy { it.id }
    val marca = pontasCortadasM.coerceAtLeast(0)
    return map { vinda ->
        porId[vinda.id]?.let { vinda.copy(polilinha = it.polilinha, pontasJaCortadasM = marca) }
            ?: vinda.copy(pontasJaCortadasM = marca)
    }
}
