package com.pedrohrsm.diariodebordo.garagem.data

import com.pedrohrsm.diariodebordo.core.preferencias.Preferencias
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * A moto em foco nas abas. Sem escolha salva (ou se ela foi excluída), vale a primeira da garagem.
 * Null quando a garagem está vazia: as telas mostram o caminho para o cadastro.
 */
class MotoAtual @Inject constructor(private val motos: MotoRepositorio, private val preferencias: Preferencias) {
    val moto: Flow<Moto?> = combine(motos.observarAtivos(), preferencias.motoSelecionada) { lista, id ->
        lista.find { it.id == id } ?: lista.firstOrNull()
    }

    fun selecionar(id: String) = preferencias.selecionarMoto(id)
}
