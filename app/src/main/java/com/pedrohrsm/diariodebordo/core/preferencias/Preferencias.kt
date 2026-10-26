package com.pedrohrsm.diariodebordo.core.preferencias

import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import kotlinx.coroutines.flow.Flow

/** Preferências do aparelho (não são dados do diário: não entram no arquivo exportado). */
interface Preferencias {
    /** Moto em foco nas abas Diário, Plano e Rotas. */
    val motoSelecionada: Flow<String?>
    fun selecionarMoto(id: String?)

    val tema: Flow<TemaDiario>
    fun definirTema(tema: TemaDiario)

    /** Texto, espaços e alvos de toque maiores, para usar com luva. */
    val modoOficina: Flow<Boolean>
    fun definirModoOficina(ligado: Boolean)
}
