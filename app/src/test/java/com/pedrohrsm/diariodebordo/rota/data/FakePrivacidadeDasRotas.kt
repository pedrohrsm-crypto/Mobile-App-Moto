package com.pedrohrsm.diariodebordo.rota.data

import com.pedrohrsm.diariodebordo.rota.dominio.CorteDasPontas
import com.pedrohrsm.diariodebordo.rota.dominio.ProtecaoDasRotas
import com.pedrohrsm.diariodebordo.rota.dominio.ZonaDePrivacidade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Proteção em memória. [falhar] simula zonas ilegíveis: a leitura lança, como no aparelho. */
class FakePrivacidadeDasRotas(
    inicial: ProtecaoDasRotas = ProtecaoDasRotas(CorteDasPontas.DESLIGADO),
    private val segredoFixo: ByteArray = ByteArray(32) { it.toByte() },
) : PrivacidadeDasRotas {
    val estado = MutableStateFlow(inicial)
    var falhar = false

    override val protecao: Flow<ProtecaoDasRotas> = estado.map { ler() }

    private fun ler(): ProtecaoDasRotas {
        if (falhar) throw ZonasIlegiveisException()
        return estado.value
    }

    override suspend fun atual(): ProtecaoDasRotas = ler()

    override suspend fun segredo(): ByteArray = segredoFixo

    override suspend fun salvarCorte(corte: CorteDasPontas) {
        estado.value = estado.value.copy(corte = corte)
    }

    override suspend fun salvarZona(zona: ZonaDePrivacidade) {
        val zonas = estado.value.zonas
        estado.value = estado.value.copy(
            zonas = if (zonas.any { it.id == zona.id }) {
                zonas.map {
                    if (it.id ==
                        zona.id
                    ) {
                        zona
                    } else {
                        it
                    }
                }
            } else {
                zonas + zona
            },
        )
    }

    override suspend fun excluirZona(id: String) {
        val zonas = estado.value.zonas
        check(zonas.any { it.id == id }) { "Zona de privacidade não encontrada" }
        estado.value = estado.value.copy(zonas = zonas.filter { it.id != id })
    }
}
