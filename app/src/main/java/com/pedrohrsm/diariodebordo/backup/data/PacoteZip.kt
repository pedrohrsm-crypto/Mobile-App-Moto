package com.pedrohrsm.diariodebordo.backup.data

import com.pedrohrsm.diariodebordo.backup.dominio.ArquivoInvalidoException
import com.pedrohrsm.diariodebordo.backup.dominio.CopiaDeSeguranca
import com.pedrohrsm.diariodebordo.backup.dominio.FORMATO_ATUAL
import com.pedrohrsm.diariodebordo.backup.dominio.FormatoNovoDemaisException
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Formato do arquivo: um .zip com diario.json e as fotos em fotos/. Lê com limites de tamanho e só
 * aceita nomes simples de foto, porque o arquivo pode ter vindo de qualquer lugar (WhatsApp, Drive).
 */
object PacoteZip {
    const val DADOS = "diario.json"
    const val PASTA_FOTOS = "fotos/"

    private const val MAXIMO_DADOS = 50L * 1024 * 1024
    private const val MAXIMO_FOTO = 20L * 1024 * 1024
    private const val MAXIMO_ENTRADAS = 20_000
    private val NOME_SEGURO = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,99}")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun nomeDeFotoValido(nome: String): Boolean = NOME_SEGURO.matches(nome) && ".." !in nome

    /** [abrirFoto] devolve null para foto que sumiu do aparelho: o backup sai sem ela. */
    fun escrever(saida: OutputStream, copia: CopiaDeSeguranca, abrirFoto: (String) -> InputStream?): List<String> {
        val faltando = mutableListOf<String>()
        ZipOutputStream(saida).use { zip ->
            zip.putNextEntry(ZipEntry(DADOS))
            zip.write(json.encodeToString(CopiaDeSeguranca.serializer(), copia).toByteArray())
            zip.closeEntry()
            copia.fotos.map { it.arquivo }.distinct().filter(::nomeDeFotoValido).forEach { nome ->
                val entrada = abrirFoto(nome)
                if (entrada == null) {
                    faltando += nome
                } else {
                    zip.putNextEntry(ZipEntry(PASTA_FOTOS + nome))
                    entrada.use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        return faltando
    }

    /**
     * Lê o pacote. Cada foto é entregue a [guardarFoto] (nome, conteúdo) na ordem do zip; os dados
     * voltam no fim. Formato mais novo que o do app é recusado sem gravar nada no banco.
     */
    fun ler(entrada: InputStream, guardarFoto: (String, InputStream) -> Unit): CopiaDeSeguranca {
        var dados: ByteArray? = null
        var entradas = 0
        try {
            ZipInputStream(entrada).use { zip ->
                while (true) {
                    val item = zip.nextEntry ?: break
                    if (++entradas > MAXIMO_ENTRADAS) throw ArquivoInvalidoException("Arquivo com itens demais")
                    when {
                        item.isDirectory -> Unit
                        item.name == DADOS -> dados = lerLimitado(zip, MAXIMO_DADOS)
                        item.name.startsWith(PASTA_FOTOS) -> {
                            val nome = item.name.removePrefix(PASTA_FOTOS)
                            if (!nomeDeFotoValido(nome)) throw ArquivoInvalidoException("Nome de foto recusado: $nome")
                            guardarFoto(nome, lerLimitado(zip, MAXIMO_FOTO).inputStream())
                        }
                        // Entradas desconhecidas são ignoradas: nada fora de fotos/ é gravado.
                        else -> Unit
                    }
                    zip.closeEntry()
                }
            }
        } catch (e: ZipException) {
            throw ArquivoInvalidoException("Não é um arquivo .zip válido", e)
        }
        val bytes = dados ?: throw ArquivoInvalidoException("Arquivo sem $DADOS")
        val copia = try {
            json.decodeFromString(CopiaDeSeguranca.serializer(), bytes.decodeToString())
        } catch (e: SerializationException) {
            throw ArquivoInvalidoException("Dados ilegíveis", e)
        } catch (e: IllegalArgumentException) {
            throw ArquivoInvalidoException("Dados ilegíveis", e)
        }
        if (copia.formato > FORMATO_ATUAL) throw FormatoNovoDemaisException(copia.formato)
        return copia
    }

    /** Lê até [limite] bytes; acima disso recusa (proteção contra zip que se expande sem fim). */
    private fun lerLimitado(entrada: InputStream, limite: Long): ByteArray {
        val saida = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val lidos = entrada.read(buffer)
            if (lidos < 0) break
            total += lidos
            if (total > limite) throw ArquivoInvalidoException("Item maior que o permitido")
            saida.write(buffer, 0, lidos)
        }
        return saida.toByteArray()
    }
}
