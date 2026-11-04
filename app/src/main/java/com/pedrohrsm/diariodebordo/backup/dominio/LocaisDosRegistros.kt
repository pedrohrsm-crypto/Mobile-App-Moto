package com.pedrohrsm.diariodebordo.backup.dominio

import com.pedrohrsm.diariodebordo.diario.data.Registro

/**
 * Risco #23 (decisão do dono, R3, em 4-Nov-2026): o local de um registro pode mostrar onde a pessoa mora, e o
 * arquivo entra no backup do Android. Sem a escolha dela, o registro vai para o arquivo sem latitude e longitude;
 * o resto vai inteiro. É a única regra do que sai do registro quando o local fica de fora.
 */
fun Registro.paraOArquivo(comLocais: Boolean): Registro =
    if (comLocais) this else copy(latitude = null, longitude = null)

/**
 * Decisão do PM (4-Nov-2026): um arquivo sem locais ([locaisOmitidos]) não apaga o local que o aparelho já tem. Cada
 * registro que chega fica com a latitude e a longitude do registro de mesmo id no aparelho; o que o aparelho não tem
 * chega igual. Arquivo com locais chega como está.
 */
fun List<Registro>.paraOAparelho(locaisOmitidos: Boolean, doAparelho: List<Registro>): List<Registro> {
    if (!locaisOmitidos) return this
    val porId = doAparelho.associateBy { it.id }
    return map { vindo ->
        porId[vindo.id]?.let { vindo.copy(latitude = it.latitude, longitude = it.longitude) } ?: vindo
    }
}
