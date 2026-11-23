package com.pedrohrsm.diariodebordo.diario.fotos

import com.pedrohrsm.diariodebordo.diario.data.FotoRegistroDao
import com.pedrohrsm.diariodebordo.diario.dominio.fotosOrfas
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Tira da pasta do app os arquivos de foto sem registro ativo (#94, risco #79), pela regra de [fotosOrfas].
 * Ordem: lista a pasta, lê as fotos ativas do Room e só então apaga. Se a pasta ou o banco não puderem ser lidos,
 * a exceção sobe e nada é apagado. A falha de um arquivo não para os outros: é contada em [Resultado.Feita.falharam]
 * e o arquivo continua órfão, para a próxima varredura. Faz leitura de disco: quem chama roda fora da thread principal.
 */
class VarreduraDeFotosOrfas @Inject constructor(
    private val dao: FotoRegistroDao,
    private val fotos: GuardaFotos,
    private val trava: TravaDasFotos,
    private val relogio: Clock,
) {
    sealed interface Resultado {
        /** A restauração do backup está com a trava: nada foi lido nem apagado. */
        data object Adiada : Resultado

        data class Feita(val apagadas: Int, val falharam: Int) : Resultado
    }

    suspend fun varrer(): Resultado = trava.seLivre { varrerComATrava() } ?: Resultado.Adiada

    private suspend fun varrerComATrava(): Resultado.Feita {
        val naPasta = fotos.listar()
        val ativas = dao.arquivosAtivos().toSet()
        val orfas = fotosOrfas(naPasta, ativas, relogio.millis())
        val apagou = orfas.map { arquivo -> tentarApagar(arquivo) }
        return Resultado.Feita(apagadas = apagou.count { it }, falharam = apagou.count { !it })
    }

    private fun tentarApagar(arquivo: String): Boolean = try {
        fotos.apagar(arquivo)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // SecurityException ou erro de E/S: o arquivo fica para a próxima varredura e a falha entra na contagem.
        false
    }
}
