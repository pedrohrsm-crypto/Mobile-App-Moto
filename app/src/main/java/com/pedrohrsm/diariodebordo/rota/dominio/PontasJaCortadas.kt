package com.pedrohrsm.diariodebordo.rota.dominio

/**
 * Risco #88, resposta 1 (dono, 22-Nov-2026): a rota que chegou ao aparelho já sem as pontas não perde as pontas
 * de novo a cada exportação. [pontasJaCortadasM] é o tamanho escolhido (sem a variação de [fatorDoCorte]) do corte que
 * a polilinha salva já sofreu; 0 = a polilinha tem as pontas reais, como toda rota gravada no aparelho.
 *
 * É a única regra de quando o corte das pontas é pulado; a exportação (`paraOArquivo`) e o detalhe da rota leem
 * daqui. O corte só é pulado se o que já foi cortado cobre o tamanho escolhido agora: a pessoa que sobe o corte de
 * 1 km para 5 km tem de ver 5 km ocultos, então a rota cortada com 1 km é cortada outra vez. As zonas de
 * privacidade valem sempre, porque uma zona nova tem de esconder o trecho mesmo numa rota antiga.
 */
fun ProtecaoDasRotas.paraOTracadoJaCortado(pontasJaCortadasM: Int): ProtecaoDasRotas =
    if (corte != CorteDasPontas.DESLIGADO && pontasJaCortadasM >= corte.metros) {
        copy(corte = CorteDasPontas.DESLIGADO)
    } else {
        this
    }
