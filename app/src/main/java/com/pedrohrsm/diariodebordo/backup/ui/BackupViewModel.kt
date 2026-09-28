package com.pedrohrsm.diariodebordo.backup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.backup.data.BackupRepositorio
import com.pedrohrsm.diariodebordo.backup.data.CanalDeDocumentos
import com.pedrohrsm.diariodebordo.backup.data.ResumoExportacao
import com.pedrohrsm.diariodebordo.backup.data.ResumoImportacao
import com.pedrohrsm.diariodebordo.backup.dominio.ArquivoInvalidoException
import com.pedrohrsm.diariodebordo.backup.dominio.FormatoNovoDemaisException
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ResultadoBackup {
    data class Exportado(val resumo: ResumoExportacao) : ResultadoBackup
    data class Importado(val resumo: ResumoImportacao) : ResultadoBackup
    data object ArquivoInvalido : ResultadoBackup
    data object VersaoNova : ResultadoBackup
    data class Falha(val detalhe: String?) : ResultadoBackup
}

data class BackupEstado(
    /** Aviso de dados pessoais aberto, antes de escolher onde salvar. */
    val confirmandoExportacao: Boolean = false,
    val comRotas: Boolean = true,
    val ocupado: Boolean = false,
    val resultado: ResultadoBackup? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backup: BackupRepositorio,
    private val canal: CanalDeDocumentos,
    private val relogio: Clock,
) : ViewModel() {

    private val _estado = MutableStateFlow(BackupEstado())
    val estado: StateFlow<BackupEstado> = _estado.asStateFlow()

    /** Nome sugerido no seletor do sistema, com a data para não sobrescrever backups antigos. */
    fun nomeSugerido(): String = "diario-de-bordo-${LocalDate.now(relogio)}.zip"

    fun pedirExportacao() = _estado.update { it.copy(confirmandoExportacao = true, resultado = null) }

    fun alternarRotas(comRotas: Boolean) = _estado.update { it.copy(comRotas = comRotas) }

    fun cancelarExportacao() = _estado.update { it.copy(confirmandoExportacao = false) }

    /** Chamado com o destino escolhido; null = a pessoa fechou o seletor. */
    fun exportar(uri: String?) {
        val comRotas = _estado.value.comRotas
        _estado.update { it.copy(confirmandoExportacao = false) }
        if (uri == null) return
        executar { ResultadoBackup.Exportado(canal.escrever(uri) { backup.exportar(it, comRotas) }) }
    }

    fun importar(uri: String?) {
        if (uri == null) return
        executar { ResultadoBackup.Importado(canal.ler(uri) { backup.importar(it) }) }
    }

    fun fecharResultado() = _estado.update { it.copy(resultado = null) }

    private fun executar(acao: suspend () -> ResultadoBackup) {
        if (_estado.value.ocupado) return
        _estado.update { it.copy(ocupado = true, resultado = null) }
        viewModelScope.launch {
            val resultado = try {
                acao()
            } catch (e: ArquivoInvalidoException) {
                ResultadoBackup.ArquivoInvalido
            } catch (e: FormatoNovoDemaisException) {
                ResultadoBackup.VersaoNova
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ResultadoBackup.Falha(e.message)
            }
            _estado.update { it.copy(ocupado = false, resultado = resultado) }
        }
    }
}
