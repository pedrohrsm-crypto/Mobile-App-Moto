package com.pedrohrsm.diariodebordo.backup.data

import com.pedrohrsm.diariodebordo.backup.dominio.ArquivoInvalidoException
import com.pedrohrsm.diariodebordo.backup.dominio.CopiaDeSeguranca
import com.pedrohrsm.diariodebordo.backup.dominio.EspacoInsuficienteException
import com.pedrohrsm.diariodebordo.backup.dominio.conferir
import com.pedrohrsm.diariodebordo.backup.dominio.jpegSemMetadados
import com.pedrohrsm.diariodebordo.backup.dominio.nomeDeFotoValido
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
 * Tetos de uma leitura. O total de fotos é o que a importação pode gravar em disco, de uma vez. Os dados ficam
 * inteiros na memória antes do JSON: 10 MB cabem num aparelho modesto sem `OutOfMemoryError`, que não é Exception.
 */
data class LimitesDoPacote(
    val dadosBytes: Long = 10L * 1024 * 1024,
    val fotoBytes: Long = 20L * 1024 * 1024,
    val entradas: Int = 20_000,
    val totalDeFotosBytes: Long = 1024L * 1024 * 1024,
)

/**
 * Formato do arquivo: um .zip com diario.json primeiro e as fotos em fotos/. O formato é estrito: qualquer outra
 * entrada (pasta, caminho com `../` ou começando em `/`, nome de foto com caminho) recusa o arquivo inteiro. Toda
 * entrada é lida com teto, porque o arquivo pode ter vindo de qualquer lugar (WhatsApp, Drive).
 */
object PacoteZip {
    const val DADOS = "diario.json"
    const val PASTA_FOTOS = "fotos/"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

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

    /** Só os dados conferidos (primeira entrada), sem ler as fotos: para saber, antes, se a importação pede escolha. */
    fun lerDados(entrada: InputStream, limites: LimitesDoPacote = LimitesDoPacote()): CopiaDeSeguranca =
        abrir(entrada) { zip -> dadosDaPrimeiraEntrada(zip, limites) }

    /**
     * Lê o pacote inteiro. Os dados vêm primeiro, conferidos, e passam por [preparar], que devolve a cópia que vai
     * ser gravada. Depois, só as fotos que essa cópia usa chegam a [guardarFoto] (nome, JPEG sem metadados), e o
     * total delas nunca passa de [LimitesDoPacote.totalDeFotosBytes]. Formato mais novo é recusado sem gravar nada.
     */
    fun ler(
        entrada: InputStream,
        limites: LimitesDoPacote = LimitesDoPacote(),
        preparar: (CopiaDeSeguranca) -> CopiaDeSeguranca = { it },
        guardarFoto: (String, ByteArray) -> Unit,
    ): CopiaDeSeguranca = abrir(entrada) { zip ->
        val copia = preparar(dadosDaPrimeiraEntrada(zip, limites))
        val usadas = copia.fotos.map { it.arquivo }.toSet()
        var entradas = 1
        var totalFotos = 0L
        val vistas = mutableSetOf(DADOS)
        while (true) {
            val item = zip.nextEntry ?: break
            if (++entradas > limites.entradas) throw ArquivoInvalidoException("Arquivo com itens demais")
            if (!vistas.add(item.name)) throw ArquivoInvalidoException("Entrada repetida no arquivo")
            if (item.isDirectory || !item.name.startsWith(PASTA_FOTOS)) {
                throw ArquivoInvalidoException("Entrada fora do formato do backup")
            }
            val nome = item.name.removePrefix(PASTA_FOTOS)
            if (!nomeDeFotoValido(nome)) throw ArquivoInvalidoException("Nome de foto recusado")
            // A foto é lida com teto mesmo quando a cópia não a usa; só a usada é gravada.
            val bytes = lerLimitado(zip, limites.fotoBytes)
            if (nome in usadas) {
                val foto = jpegSemMetadados(bytes)
                totalFotos += foto.size
                if (totalFotos > limites.totalDeFotosBytes) {
                    throw EspacoInsuficienteException(limites.totalDeFotosBytes)
                }
                guardarFoto(nome, foto)
            }
            zip.closeEntry()
        }
        copia
    }

    private fun <T> abrir(entrada: InputStream, bloco: (ZipInputStream) -> T): T = try {
        ZipInputStream(entrada).use(bloco)
    } catch (e: ZipException) {
        throw ArquivoInvalidoException("Não é um arquivo .zip válido", e)
    }

    private fun dadosDaPrimeiraEntrada(zip: ZipInputStream, limites: LimitesDoPacote): CopiaDeSeguranca {
        val primeira = zip.nextEntry
        if (primeira == null || primeira.name != DADOS) throw ArquivoInvalidoException("Arquivo sem $DADOS no início")
        val bytes = lerLimitado(zip, limites.dadosBytes)
        zip.closeEntry()
        val copia = try {
            json.decodeFromString(CopiaDeSeguranca.serializer(), bytes.decodeToString())
        } catch (e: SerializationException) {
            throw ArquivoInvalidoException("Dados ilegíveis", e)
        } catch (e: IllegalArgumentException) {
            throw ArquivoInvalidoException("Dados ilegíveis", e)
        }
        return conferir(copia)
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
