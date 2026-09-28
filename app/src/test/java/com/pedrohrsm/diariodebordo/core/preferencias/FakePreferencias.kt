package com.pedrohrsm.diariodebordo.core.preferencias

import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePreferencias : Preferencias {
    private val selecionada = MutableStateFlow<String?>(null)
    override val motoSelecionada: Flow<String?> = selecionada
    override fun selecionarMoto(id: String?) {
        selecionada.value = id
    }

    private val temaAtual = MutableStateFlow(TemaDiario.OFICINA)
    override val tema: Flow<TemaDiario> = temaAtual
    override fun definirTema(tema: TemaDiario) {
        temaAtual.value = tema
    }

    private val oficina = MutableStateFlow(false)
    override val modoOficina: Flow<Boolean> = oficina
    override fun definirModoOficina(ligado: Boolean) {
        oficina.value = ligado
    }
}
