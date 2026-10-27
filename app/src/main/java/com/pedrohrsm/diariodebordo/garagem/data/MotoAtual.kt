package com.pedrohrsm.diariodebordo.garagem.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * A moto em foco nas abas. Sem escolha (ou se ela foi excluída), vale a primeira da garagem.
 * Null quando a garagem está vazia: as telas mostram o caminho para o cadastro.
 * A escolha vive na memória do processo; guardá-la entre aberturas do app é da frente de preferências.
 */
@Singleton
class MotoAtual @Inject constructor(private val motos: MotoRepositorio) {
    private val selecionada = MutableStateFlow<String?>(null)

    val moto: Flow<Moto?> = combine(motos.observarAtivos(), selecionada) { lista, id ->
        lista.find { it.id == id } ?: lista.firstOrNull()
    }

    fun selecionar(id: String) {
        selecionada.value = id
    }
}
