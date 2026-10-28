package com.pedrohrsm.diariodebordo.backup.data

import com.pedrohrsm.diariodebordo.backup.dominio.ArquivoInvalidoException
import com.pedrohrsm.diariodebordo.backup.dominio.CopiaDeSeguranca
import com.pedrohrsm.diariodebordo.backup.dominio.EspacoInsuficienteException
import com.pedrohrsm.diariodebordo.backup.dominio.FormatoNovoDemaisException
import com.pedrohrsm.diariodebordo.backup.dominio.nomeDeFotoValido
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistro
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.CategoriaMoto
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PacoteZipTest {

    private val copia = CopiaDeSeguranca(
        geradoEm = 1_000,
        fusoHorario = "America/Sao_Paulo",
        motos = listOf(
            Moto(
                id = "m",
                apelido = "Bandida",
                marca = "Harley-Davidson",
                modelo = "883",
                categoria = CategoriaMoto.CUSTOM,
            ),
        ),
        registros = listOf(
            Registro(id = "r", motoId = "m", tipo = TipoRegistro.MANUTENCAO, titulo = "Óleo", dataEm = 5),
        ),
        fotos = listOf(FotoRegistro(id = "f", registroId = "r", arquivo = "a.jpg", ordem = 0)),
    )

    private val dadosJson = """{"geradoEm":1,"fusoHorario":"UTC"}""".toByteArray()

    private fun zip(vararg itens: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { saida ->
        ZipOutputStream(saida).use { z ->
            itens.forEach { (nome, bytes) ->
                z.putNextEntry(ZipEntry(nome))
                z.write(bytes)
                z.closeEntry()
            }
        }
    }.toByteArray()

    /**
     * Zip com nome repetido. A segunda ocorrência sai com um nome provisório do mesmo tamanho (última letra trocada)
     * e o nome é trocado nos bytes depois, no cabeçalho local e no diretório central.
     */
    private fun zipComRepetida(vararg itens: Pair<String, ByteArray>): ByteArray {
        val vistos = mutableSetOf<String>()
        val trocas = mutableListOf<Pair<String, String>>()
        val provisorios = itens.map { (nome, bytes) ->
            if (vistos.add(nome)) {
                nome to bytes
            } else {
                val provisorio = nome.dropLast(1) + "Q"
                trocas += provisorio to nome
                provisorio to bytes
            }
        }
        var bytes = zip(*provisorios.toTypedArray())
        trocas.forEach { (de, para) -> bytes = trocar(bytes, de.toByteArray(), para.toByteArray()) }
        return bytes
    }

    private fun trocar(bytes: ByteArray, de: ByteArray, para: ByteArray): ByteArray {
        val saida = bytes.copyOf()
        var i = 0
        var trocas = 0
        while (i <= saida.size - de.size) {
            if (de.indices.all { saida[i + it] == de[it] }) {
                para.copyInto(saida, i)
                trocas++
                i += de.size
            } else {
                i++
            }
        }
        check(trocas == 2) { "o nome devia aparecer no cabeçalho local e no diretório central ($trocas)" }
        return saida
    }

    private fun exportado(foto: ByteArray?): ByteArray = ByteArrayOutputStream().also { saida ->
        PacoteZip.escrever(saida, copia) { if (it == "a.jpg") foto?.inputStream() else null }
    }.toByteArray()

    @Test
    fun idaEVoltaComFotos() {
        val fotos = mutableMapOf<String, ByteArray>()
        val lida = PacoteZip.ler(exportado(JPEG).inputStream()) { nome, conteudo -> fotos[nome] = conteudo }
        assertEquals(copia, lida)
        assertArrayEquals(JPEG, fotos["a.jpg"])
    }

    @Test
    fun fotoQueSumiuFicaDeForaEAvisa() {
        val faltando = PacoteZip.escrever(ByteArrayOutputStream(), copia) { null }
        assertEquals(listOf("a.jpg"), faltando)
    }

    @Test(expected = ArquivoInvalidoException::class)
    fun nomeDeFotoComCaminhoEhRecusado() {
        val dados = zip(PacoteZip.DADOS to dadosJson, "fotos/../../databases/diario.db" to byteArrayOf(0))
        PacoteZip.ler(dados.inputStream()) { _, _ -> error("não deveria gravar") }
    }

    @Test
    fun nomesDeFotoAceitos() {
        assertTrue(nomeDeFotoValido("0b7e2f7c-3c1d-4d0a-9d1e-2a3b4c5d6e7f.jpg"))
        assertFalse(nomeDeFotoValido(".oculto"))
        assertFalse(nomeDeFotoValido("a/b.jpg"))
        assertFalse(nomeDeFotoValido("a..jpg"))
    }

    @Test(expected = ArquivoInvalidoException::class)
    fun arquivoQueNaoEhZip() {
        PacoteZip.ler("isto não é um zip".toByteArray().inputStream()) { _, _ -> }
    }

    @Test(expected = ArquivoInvalidoException::class)
    fun zipSemDadosNoInicio() {
        PacoteZip.ler(zip("fotos/a.jpg" to JPEG, PacoteZip.DADOS to dadosJson).inputStream()) { _, _ -> }
    }

    @Test(expected = ArquivoInvalidoException::class)
    fun jsonQuebrado() {
        PacoteZip.ler(zip(PacoteZip.DADOS to "{".toByteArray()).inputStream()) { _, _ -> }
    }

    @Test(expected = FormatoNovoDemaisException::class)
    fun formatoDeVersaoMaisNovaEhRecusado() {
        val json = """{"formato":99,"geradoEm":1,"fusoHorario":"UTC","campoNovo":1}""".toByteArray()
        PacoteZip.ler(zip(PacoteZip.DADOS to json).inputStream()) { _, _ -> }
    }

    @Test
    fun itemQueSeExpandeAlemDoLimiteEhRecusado() {
        // JSON válido com 4 MB de espaços comprime para poucos KB: o tamanho é contado na leitura, não no cabeçalho.
        val bomba = zip(PacoteZip.DADOS to dadosJson + ByteArray(4 * 1024 * 1024) { ' '.code.toByte() })
        try {
            PacoteZip.ler(bomba.inputStream(), LimitesDoPacote(dadosBytes = 1024 * 1024)) { _, _ -> }
            fail("deveria recusar o item grande demais")
        } catch (e: ArquivoInvalidoException) {
            assertEquals("Item maior que o permitido", e.message)
        }
    }

    @Test
    fun fotosAcimaDoTotalDaImportacaoParamAntesDeGravar() {
        val gravadas = mutableListOf<String>()
        val duas = copia.copy(
            fotos =
            copia.fotos + FotoRegistro(id = "g", registroId = "r", arquivo = "b.jpg", ordem = 1),
        )
        val arquivo = ByteArrayOutputStream().also { saida ->
            PacoteZip.escrever(saida, duas) { JPEG.inputStream() }
        }.toByteArray()
        try {
            PacoteZip.ler(arquivo.inputStream(), LimitesDoPacote(totalDeFotosBytes = JPEG.size + 1L)) { nome, _ ->
                gravadas += nome
            }
            error("deveria parar no total")
        } catch (e: EspacoInsuficienteException) {
            assertEquals(listOf("a.jpg"), gravadas)
        }
    }

    @Test
    fun fotoQueACopiaNaoUsaNaoEhGravada() {
        val semFotos = ByteArrayOutputStream().also { PacoteZip.escrever(it, copia.copy(fotos = emptyList())) { null } }
        val pacote = zip(
            PacoteZip.DADOS to dadosDe(semFotos.toByteArray()),
            "fotos/intrusa.jpg" to JPEG,
        )
        val gravadas = mutableListOf<String>()
        PacoteZip.ler(pacote.inputStream()) { nome, _ -> gravadas += nome }
        assertEquals(emptyList<String>(), gravadas)
    }

    @Test
    fun fotoMaiorQueOTetoEhRecusadaSemGravar() {
        val gravadas = mutableListOf<String>()
        val grande = JPEG + ByteArray(2 * 1024) // a foto que a cópia usa, passando do teto por foto
        val pacote = zip(PacoteZip.DADOS to dadosDe(exportado(null)), "fotos/a.jpg" to grande)
        try {
            PacoteZip.ler(pacote.inputStream(), LimitesDoPacote(fotoBytes = 1024)) { nome, _ -> gravadas += nome }
            fail("deveria recusar a foto grande demais")
        } catch (e: ArquivoInvalidoException) {
            assertEquals("Item maior que o permitido", e.message)
        }
        assertEquals(emptyList<String>(), gravadas)
    }

    @Test
    fun fotoNaoUsadaMaiorQueOTetoTambemEhRecusada() {
        // A cópia usa só a.jpg; z.jpg não seria gravada, mas é lida com o mesmo teto (zip bomb escondida).
        val pacote = zip(PacoteZip.DADOS to dadosDe(exportado(null)), "fotos/z.jpg" to ByteArray(2048))
        try {
            PacoteZip.ler(pacote.inputStream(), LimitesDoPacote(fotoBytes = 1024)) { _, _ -> }
            fail("deveria recusar a foto grande demais, mesmo sem uso")
        } catch (e: ArquivoInvalidoException) {
            assertEquals("Item maior que o permitido", e.message)
        }
    }

    @Test
    fun entradaRepetidaRecusaOArquivoInteiro() {
        val dados = dadosDe(exportado(JPEG))
        // O ZipOutputStream do Java não escreve nome repetido: a segunda entrada é montada à mão no fluxo.
        val pacote = zipComRepetida(PacoteZip.DADOS to dados, "fotos/a.jpg" to JPEG, "fotos/a.jpg" to JPEG)
        try {
            PacoteZip.ler(pacote.inputStream()) { _, _ -> }
            fail("deveria recusar a entrada repetida")
        } catch (e: ArquivoInvalidoException) {
            assertEquals("Entrada repetida no arquivo", e.message)
        }
    }

    @Test
    fun entradaForaDoFormatoRecusaOArquivoInteiro() {
        val dados = dadosDe(exportado(JPEG))
        listOf("../evil", "/evil", "outra/pasta.txt", "fotos/", "diario.json.bak").forEach { intrusa ->
            val gravadas = mutableListOf<String>()
            val pacote = zip(PacoteZip.DADOS to dados, "fotos/a.jpg" to JPEG, intrusa to byteArrayOf(1))
            try {
                PacoteZip.ler(pacote.inputStream()) { nome, _ -> gravadas += nome }
                fail("deveria recusar a entrada $intrusa")
            } catch (e: ArquivoInvalidoException) {
                assertEquals(intrusa, "Entrada fora do formato do backup", e.message)
            }
        }
    }

    /** O diario.json de um pacote. */
    private fun dadosDe(pacote: ByteArray): ByteArray = ZipInputStream(pacote.inputStream()).use { z ->
        checkNotNull(z.nextEntry)
        z.readBytes()
    }

    private companion object {
        /** Menor JPEG que o leitor aceita: SOI, um segmento JFIF e o início da imagem. */
        val JPEG = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(),
            0xFF.toByte(), 0xE0.toByte(), 0x00, 0x04, 0x4A, 0x46,
            0xFF.toByte(), 0xDA.toByte(), 0x00, 0x02, 0x01, 0x02,
        )
    }
}
