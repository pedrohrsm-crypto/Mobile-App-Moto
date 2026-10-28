package com.pedrohrsm.diariodebordo.backup.data

import com.pedrohrsm.diariodebordo.backup.dominio.CopiaDeSeguranca
import com.pedrohrsm.diariodebordo.backup.dominio.EscolhaDeMotoNecessariaException
import com.pedrohrsm.diariodebordo.backup.dominio.Mesclagem
import com.pedrohrsm.diariodebordo.backup.dominio.aplicarLimiteDeMotos
import com.pedrohrsm.diariodebordo.backup.dominio.mesclar
import com.pedrohrsm.diariodebordo.core.database.Transacao
import com.pedrohrsm.diariodebordo.core.premium.Premium
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.garagem.data.MotoRepositorio
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorLembretes
import com.pedrohrsm.diariodebordo.rota.data.Rota
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.first

data class ResumoExportacao(val motos: Int, val registros: Int, val fotos: Int, val rotas: Int, val fotosFaltando: Int)

data class ResumoImportacao(val novos: Int, val atualizados: Int, val fotos: Int)

/**
 * Backup do plano grátis: um arquivo que a pessoa guarda onde quiser, sem servidor (docs/offline-first.md).
 * Faz leitura e escrita de arquivos: quem chama roda fora da thread principal ([CanalDeDocumentos]).
 * Nada do arquivo vai para log: os erros levam só o motivo, nunca o conteúdo.
 */
class BackupRepositorio @Inject constructor(
    private val dao: BackupDao,
    private val motos: MotoRepositorio,
    private val arquivos: ArquivosDoBackup,
    private val transacao: Transacao,
    private val lembretes: AgendadorLembretes,
    private val premium: Premium,
    private val relogio: Clock,
) {
    /** Só o que está ativo. Sem rotas, os rolês continuam no diário, mas sem o traçado. */
    suspend fun montarCopia(comRotas: Boolean): CopiaDeSeguranca {
        val motos = dao.motos().filter { it.excluidoEm == null }
        val motoIds = motos.map { it.id }.toSet()
        val rotas = if (comRotas) dao.rotas().filter { it.excluidoEm == null && it.motoId in motoIds } else emptyList()
        val rotaIds = rotas.map { it.id }.toSet()
        val registros = dao.registros()
            .filter { it.excluidoEm == null && it.motoId in motoIds }
            .map { if (it.rotaId == null || it.rotaId in rotaIds) it else it.copy(rotaId = null) }
        val registroIds = registros.map { it.id }.toSet()
        return CopiaDeSeguranca(
            geradoEm = relogio.millis(),
            fusoHorario = relogio.zone.id,
            motos = motos,
            leituras = dao.leituras().filter { it.excluidoEm == null && it.motoId in motoIds },
            registros = registros,
            fotos = dao.fotos().filter { it.excluidoEm == null && it.registroId in registroIds },
            itensDoPlano = dao.itensDoPlano().filter { it.excluidoEm == null && it.motoId in motoIds },
            rotas = rotas,
        )
    }

    /**
     * Gera o .zip e guarda uma cópia em files/exportacao/ (a que o backup automático do Android leva)
     * antes de mandar para o destino escolhido pela pessoa.
     */
    suspend fun exportar(destino: OutputStream, comRotas: Boolean): ResumoExportacao {
        val copia = montarCopia(comRotas)
        val arquivo = arquivos.ultimaExportacao()
        val parcial = File(arquivo.parentFile, "${arquivo.name}.parcial")
        val faltando = parcial.outputStream().use { PacoteZip.escrever(it, copia, arquivos::abrirFoto) }
        check(parcial.renameTo(arquivo)) { "Não foi possível guardar a cópia da exportação" }
        arquivo.inputStream().use { it.copyTo(destino) }
        return ResumoExportacao(
            motos = copia.motos.size,
            registros = copia.registros.size,
            fotos = copia.fotos.map { it.arquivo }.distinct().size - faltando.size,
            rotas = copia.rotas.size,
            fotosFaltando = faltando.size,
        )
    }

    /**
     * Primeira leitura, só dos dados: as motos entre as quais a pessoa precisa escolher (decisão #13), ou vazio
     * quando a importação cabe no plano. Nada é gravado.
     */
    suspend fun motosParaEscolher(origem: InputStream): List<Moto> {
        val copia = PacoteZip.lerDados(origem)
        return try {
            aplicarLimiteDeMotos(copia, motos.observarAtivos().first(), premium.limiteDeMotos, motoQueFica = null)
            emptyList()
        } catch (e: EscolhaDeMotoNecessariaException) {
            e.candidatas
        }
    }

    /**
     * Lê o arquivo e junta com o que já existe (vale a alteração mais recente). O banco muda numa
     * transação só: se algo falhar, nada muda, e as fotos copiadas nesta importação são apagadas.
     * [motoQueFica] é a escolha da decisão #13, quando a importação passaria do limite de motos.
     */
    suspend fun importar(origem: InputStream, motoQueFica: String? = null): ResumoImportacao {
        val fotosNovas = mutableListOf<String>()
        val ativas = motos.observarAtivos().first()
        var excluir = emptyList<String>()
        val limites = LimitesDoPacote().let {
            it.copy(totalDeFotosBytes = minOf(it.totalDeFotosBytes, arquivos.espacoLivre() - RESERVA_DE_ESPACO))
        }
        return try {
            val copia = PacoteZip.ler(
                origem,
                limites,
                preparar = { lida ->
                    aplicarLimiteDeMotos(lida, ativas, premium.limiteDeMotos, motoQueFica)
                        .also { excluir = it.motosDoAparelhoAExcluir }
                        .copia
                },
            ) { nome, conteudo ->
                if (!arquivos.fotoExiste(nome)) {
                    arquivos.gravarFoto(nome, conteudo)
                    fotosNovas += nome
                }
            }
            val resumo = transacao.executar {
                excluir.forEach { motos.excluir(it) }
                gravar(copia)
            }
            lembretes.recalcularAgora()
            resumo.copy(fotos = fotosNovas.size)
        } catch (e: Exception) {
            fotosNovas.forEach { arquivos.apagarFoto(it) }
            throw e
        }
    }

    private suspend fun gravar(copia: CopiaDeSeguranca): ResumoImportacao {
        val partes = listOf(
            mesclar(dao.motos(), copia.motos, { it.id }, { it.alteradoEm }).also { dao.gravarMotos(it.gravar) },
            mesclar(dao.leituras(), copia.leituras, {
                it.id
            }, { it.alteradoEm }).also { dao.gravarLeituras(it.gravar) },
            mesclar(dao.registros(), copia.registros, { it.id }, { it.alteradoEm })
                .also { dao.gravarRegistros(it.gravar) },
            mesclar(dao.fotos(), copia.fotos, { it.id }, { it.alteradoEm }).also { dao.gravarFotos(it.gravar) },
            mesclar(dao.itensDoPlano(), copia.itensDoPlano, { it.id }, { it.alteradoEm })
                .also { dao.gravarItensDoPlano(it.gravar) },
            // Rota excluída no aparelho perdeu o traçado (risco #23): o arquivo não a traz de volta. A que grava
            // agora também não é tocada.
            mesclar(dao.todasAsRotas(), copia.rotas, { it.id }, { it.alteradoEm }, protegido = ::rotaProtegida)
                .also { dao.gravarRotas(it.gravar) },
        )
        return ResumoImportacao(
            novos = partes.sumOf(Mesclagem<*>::novos),
            atualizados = partes.sumOf(Mesclagem<*>::atualizados),
            fotos = 0,
        )
    }

    private fun rotaProtegida(rota: Rota): Boolean = rota.excluidoEm != null || rota.emGravacao

    private companion object {
        /** O aparelho fica com pelo menos isto livre depois de gravar as fotos de uma importação. */
        const val RESERVA_DE_ESPACO = 200L * 1024 * 1024
    }
}
