package com.pedrohrsm.diariodebordo.plano.data

import com.pedrohrsm.diariodebordo.core.database.Transacao
import com.pedrohrsm.diariodebordo.core.database.TransacaoFalsa
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorFalso
import java.time.Clock

/** O plano que o [com.pedrohrsm.diariodebordo.garagem.data.HodometroRepositorio] leva junto na correção (#69). */
fun planoFalso(
    relogio: Clock,
    transacao: Transacao = TransacaoFalsa(),
    dao: ItemDoPlanoDao = FakeItemDoPlanoDao(),
): PlanoRepositorio = PlanoRepositorio(dao, transacao, relogio, AgendadorFalso())
