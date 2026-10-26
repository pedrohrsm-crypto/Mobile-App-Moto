package com.pedrohrsm.diariodebordo.backup.data

import com.pedrohrsm.diariodebordo.backup.dominio.ArquivoInvalidoException
import com.pedrohrsm.diariodebordo.backup.dominio.CopiaDeSeguranca
import com.pedrohrsm.diariodebordo.backup.dominio.FormatoNovoDemaisException
import com.pedrohrsm.diariodebordo.diario.data.FotoRegistro
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.CategoriaMoto
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    private fun zip(vararg itens: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { saida ->
        ZipOutputStream(saida).use { z ->
            itens.forEach { (nome, bytes) ->
                z.putNextEntry(ZipEntry(nome))
                z.write(bytes)
                z.closeEntry()
            }
        }
    }.toByteArray()

    @Test
    fun idaEVoltaComFotos() {
        val saida = ByteArrayOutputStream()
        val faltando = PacoteZip.escrever(saida, copia) {
            if (it ==
                "a.jpg"
            ) {
                byteArrayOf(1, 2, 3).inputStream()
            } else {
                null
            }
        }
        assertEquals(emptyList<String>(), faltando)
        val fotos = mutableMapOf<String, ByteArray>()
        val lida = PacoteZip.ler(saida.toByteArray().inputStream()) { nome, conteudo ->
            fotos[nome] =
                conteudo.readBytes()
        }
        assertEquals(copia, lida)
        assertArrayEquals(byteArrayOf(1, 2, 3), fotos["a.jpg"])
    }

    @Test
    fun fotoQueSumiuFicaDeForaEAvisa() {
        val faltando = PacoteZip.escrever(ByteArrayOutputStream(), copia) { null }
        assertEquals(listOf("a.jpg"), faltando)
    }

    @Test(expected = ArquivoInvalidoException::class)
    fun nomeDeFotoComCaminhoEhRecusado() {
        val dados = zip(
            PacoteZip.DADOS to "{\"geradoEm\":1,\"fusoHorario\":\"UTC\"}".toByteArray(),
            "fotos/../../banco.db" to byteArrayOf(0),
        )
        PacoteZip.ler(dados.inputStream()) { _, _ -> error("não deveria gravar") }
    }

    @Test
    fun nomesDeFotoAceitos() {
        assertTrue(PacoteZip.nomeDeFotoValido("0b7e2f7c-3c1d-4d0a-9d1e-2a3b4c5d6e7f.jpg"))
        assertFalse(PacoteZip.nomeDeFotoValido(".oculto"))
        assertFalse(PacoteZip.nomeDeFotoValido("a/b.jpg"))
        assertFalse(PacoteZip.nomeDeFotoValido("a..jpg"))
    }

    @Test(expected = ArquivoInvalidoException::class)
    fun arquivoQueNaoEhZip() {
        PacoteZip.ler("isto não é um zip".toByteArray().inputStream()) { _, _ -> }
    }

    @Test(expected = ArquivoInvalidoException::class)
    fun zipSemDados() {
        PacoteZip.ler(zip("fotos/a.jpg" to byteArrayOf(1)).inputStream()) { _, _ -> }
    }

    @Test(expected = ArquivoInvalidoException::class)
    fun jsonQuebrado() {
        PacoteZip.ler(zip(PacoteZip.DADOS to "{".toByteArray()).inputStream()) { _, _ -> }
    }

    @Test(expected = FormatoNovoDemaisException::class)
    fun formatoDeVersaoMaisNovaEhRecusado() {
        val dados =
            zip(
                PacoteZip.DADOS to
                    "{\"formato\":99,\"geradoEm\":1,\"fusoHorario\":\"UTC\",\"campoNovo\":1}".toByteArray(),
            )
        PacoteZip.ler(dados.inputStream()) { _, _ -> }
    }
}
