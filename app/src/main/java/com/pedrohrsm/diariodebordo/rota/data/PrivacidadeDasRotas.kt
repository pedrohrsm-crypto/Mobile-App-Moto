package com.pedrohrsm.diariodebordo.rota.data

import com.pedrohrsm.diariodebordo.rota.dominio.CorteDasPontas
import com.pedrohrsm.diariodebordo.rota.dominio.ProtecaoDasRotas
import com.pedrohrsm.diariodebordo.rota.dominio.ZonaDePrivacidade
import kotlinx.coroutines.flow.Flow

/**
 * Onde moram o corte das pontas, as zonas de privacidade e o segredo do sorteio do corte (#78): nas preferências do
 * aparelho, fora do Room. Preferência não é dado do diário: não entra no arquivo exportado, no backup automático do
 * Android (backup_rules.xml só leva files/exportacao/) nem, na fase 6, na nuvem. As zonas ficam fora do arquivo
 * por construção, não por um filtro que alguém pode esquecer.
 *
 * Leitura que falha (dado ilegível) lança: nunca vira "sem zonas", que exportaria sem proteção.
 */
interface PrivacidadeDasRotas {
    /** Emite de novo a cada mudança. Falha de leitura sai como erro do Flow. */
    val protecao: Flow<ProtecaoDasRotas>

    suspend fun atual(): ProtecaoDasRotas

    /** Sorteado uma vez no aparelho e guardado; dele sai o fator do corte de cada rota (`fatorDoCorte`). */
    suspend fun segredo(): ByteArray

    suspend fun salvarCorte(corte: CorteDasPontas)

    /** Cria ou substitui a zona de mesmo id. */
    suspend fun salvarZona(zona: ZonaDePrivacidade)

    /** Lança se a zona não existe (a escrita confere o que mudou). */
    suspend fun excluirZona(id: String)
}
