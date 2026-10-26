package com.pedrohrsm.diariodebordo.backup.data

import com.pedrohrsm.diariodebordo.backup.dominio.CopiaDeSeguranca
import com.pedrohrsm.diariodebordo.backup.dominio.Mesclagem
import com.pedrohrsm.diariodebordo.backup.dominio.mesclar
import com.pedrohrsm.diariodebordo.core.database.Transacao
import com.pedrohrsm.diariodebordo.plano.lembretes.AgendadorLembretes
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import javax.inject.Inject

data class ResumoExportacao(val motos: Int, val registros: Int, val fotos: Int, val rotas: Int, val fotosFaltando: Int)

data class ResumoImportacao(val novos: Int, val atualizados: Int, val fotos: Int)

/**
 * Backup do plano grátis: um arquivo que a pessoa guarda onde quiser, sem servidor (docs/offline-first.md).
 * Faz leitura e escrita de arquivos: quem chama roda fora da thread principal ([CanalDeDocumentos]).
 */
class BackupRepositorio @Inject constructor(
    private val dao: BackupDao,
    private val arquivos: ArquivosDoBackup,
    private val transacao: Transacao,
    private val lembretes: AgendadorLembretes,
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
            .map { if (it.rotaId in rotaIds) it else it.copy(rotaId = null) }
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
            fotos = copia.fotos.size - faltando.size,
            rotas = copia.rotas.size,
            fotosFaltando = faltando.size,
        )
    }

    /**
     * Lê o arquivo e junta com o que já existe (vale a alteração mais recente). O banco muda numa
     * transação só: se algo falhar, nada muda, e as fotos copiadas nesta importação são apagadas.
     */
    suspend fun importar(origem: InputStream): ResumoImportacao {
        val fotosNovas = mutableListOf<String>()
        return try {
            val copia = PacoteZip.ler(origem) { nome, conteudo ->
                if (!arquivos.fotoExiste(nome)) {
                    arquivos.gravarFoto(nome, conteudo)
                    fotosNovas += nome
                }
            }
            val resumo = transacao.executar { gravar(copia) }
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
            mesclar(dao.registros(), copia.registros, {
                it.id
            }, { it.alteradoEm }).also { dao.gravarRegistros(it.gravar) },
            mesclar(dao.fotos(), copia.fotos, { it.id }, { it.alteradoEm }).also { dao.gravarFotos(it.gravar) },
            mesclar(dao.itensDoPlano(), copia.itensDoPlano, { it.id }, { it.alteradoEm })
                .also { dao.gravarItensDoPlano(it.gravar) },
            mesclar(dao.rotas(), copia.rotas, { it.id }, { it.alteradoEm }).also { dao.gravarRotas(it.gravar) },
        )
        return ResumoImportacao(
            novos = partes.sumOf(Mesclagem<*>::novos),
            atualizados = partes.sumOf(Mesclagem<*>::atualizados),
            fotos = 0,
        )
    }
}
