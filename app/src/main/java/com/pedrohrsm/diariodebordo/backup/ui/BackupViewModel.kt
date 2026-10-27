package com.pedrohrsm.diariodebordo.backup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedrohrsm.diariodebordo.backup.data.BackupRepositorio
import com.pedrohrsm.diariodebordo.backup.data.CanalDeDocumentos
import com.pedrohrsm.diariodebordo.backup.data.ResumoExportacao
import com.pedrohrsm.diariodebordo.backup.data.ResumoImportacao
import com.pedrohrsm.diariodebordo.backup.dominio.ArquivoInvalidoException
import com.pedrohrsm.diariodebordo.backup.dominio.EscolhaDeMotoNecessariaException
import com.pedrohrsm.diariodebordo.backup.dominio.EspacoInsuficienteException
import com.pedrohrsm.diariodebordo.backup.dominio.FormatoNovoDemaisException
import com.pedrohrsm.diariodebordo.garagem.data.Moto
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
    data object SemEspaco : ResultadoBackup
    data class Falha(val detalhe: String?) : ResultadoBackup
}

/** Decisão #13: o arquivo deixaria mais motos do que o plano permite; a pessoa escolhe qual fica. */
data class EscolhaDeMoto(val uri: String, val candidatas: List<Moto>)

data class BackupEstado(
    /** Aviso de dados pessoais aberto, antes de escolher onde salvar. */
    val confirmandoExportacao: Boolean = false,
    /** Privacidade por padrão (risco #23, decisão do PM em 27-Out-2026): as rotas só saem se a pessoa marcar. */
    val comRotas: Boolean = false,
    val escolhendoMoto: EscolhaDeMoto? = null,
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

    /** Lê primeiro só os dados: se a importação passaria do limite de motos, pergunta qual fica antes de gravar. */
    fun importar(uri: String?) {
        if (uri == null) return
        executar {
            val candidatas = canal.ler(uri) { backup.motosParaEscolher(it) }
            if (candidatas.isEmpty()) {
                ResultadoBackup.Importado(canal.ler(uri) { backup.importar(it) })
            } else {
                _estado.update { it.copy(escolhendoMoto = EscolhaDeMoto(uri, candidatas)) }
                null
            }
        }
    }

    fun escolherMoto(motoId: String) {
        val escolha = _estado.value.escolhendoMoto ?: return
        _estado.update { it.copy(escolhendoMoto = null) }
        executar {
            try {
                ResultadoBackup.Importado(canal.ler(escolha.uri) { backup.importar(it, motoId) })
            } catch (e: EscolhaDeMotoNecessariaException) {
                // O arquivo mudou entre as duas leituras: pergunta de novo, com as motos de agora.
                _estado.update { it.copy(escolhendoMoto = escolha.copy(candidatas = e.candidatas)) }
                null
            }
        }
    }

    fun cancelarEscolha() = _estado.update { it.copy(escolhendoMoto = null) }

    fun fecharResultado() = _estado.update { it.copy(resultado = null) }

    /** [acao] devolve null quando ainda falta uma resposta da pessoa (a escolha da moto). */
    private fun executar(acao: suspend () -> ResultadoBackup?) {
        if (_estado.value.ocupado) return
        _estado.update { it.copy(ocupado = true, resultado = null) }
        viewModelScope.launch {
            val resultado = try {
                acao()
            } catch (e: ArquivoInvalidoException) {
                ResultadoBackup.ArquivoInvalido
            } catch (e: FormatoNovoDemaisException) {
                ResultadoBackup.VersaoNova
            } catch (e: EspacoInsuficienteException) {
                ResultadoBackup.SemEspaco
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ResultadoBackup.Falha(e.message)
            }
            _estado.update { it.copy(ocupado = false, resultado = resultado) }
        }
    }
}
