package com.pedrohrsm.diariodebordo.backup.dominio

import com.pedrohrsm.diariodebordo.diario.data.FotoRegistro
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.rota.data.Rota
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ConferenciaDaCopiaTest {

    private val moto = Moto(id = "m", apelido = "Bandida", marca = "Harley-Davidson", modelo = "883")
    private val copia = CopiaDeSeguranca(
        geradoEm = 1,
        fusoHorario = "UTC",
        motos = listOf(moto),
        leituras = listOf(LeituraHodometro(id = "l", motoId = "m", km = 1)),
        registros = listOf(
            Registro(id = "r", motoId = "m", tipo = TipoRegistro.ROLE, titulo = "Rolê", dataEm = 1, rotaId = "rota"),
        ),
        fotos = listOf(FotoRegistro(id = "f", registroId = "r", arquivo = "a.jpg", ordem = 0)),
        itensDoPlano = listOf(ItemDoPlano(id = "i", motoId = "m", nome = "Óleo")),
        rotas = listOf(Rota(id = "rota", motoId = "m", iniciadaEm = 1, emGravacao = false, polilinha = "_p~iF~ps|U")),
    )

    private fun recusa(copia: CopiaDeSeguranca, trecho: String) {
        try {
            conferir(copia)
            fail("deveria recusar: $trecho")
        } catch (e: ArquivoInvalidoException) {
            assertTrue(e.message, e.message.orEmpty().contains(trecho))
        }
    }

    @Test
    fun copiaInteiraPassaSemMudar() {
        assertEquals(copia, conferir(copia))
    }

    @Test
    fun rotaExcluidaChegaSemTrajeto() {
        val excluida = copia.copy(rotas = copia.rotas.map { it.copy(excluidoEm = 5) })
        assertEquals("", conferir(excluida).rotas.single().polilinha)
    }

    @Test
    fun registroCujaRotaNaoVeioFicaSemARota() {
        assertNull(conferir(copia.copy(rotas = emptyList())).registros.single().rotaId)
    }

    @Test
    fun filhoSemPaiNoArquivoEhRecusado() {
        recusa(copia.copy(leituras = listOf(LeituraHodometro(id = "x", motoId = "outra", km = 1))), "fora do arquivo")
        recusa(
            copia.copy(fotos = listOf(FotoRegistro(id = "x", registroId = "nada", arquivo = "a.jpg", ordem = 0))),
            "fora do arquivo",
        )
    }

    @Test
    fun idVazioOuRepetidoEhRecusado() {
        recusa(copia.copy(motos = listOf(moto, moto)), "repetido")
        recusa(copia.copy(itensDoPlano = listOf(ItemDoPlano(id = " ", motoId = "m", nome = "x"))), "vazio")
    }

    @Test
    fun nomeDeFotoComCaminhoRotaEmGravacaoECoordenadaImpossivelSaoRecusados() {
        recusa(copia.copy(fotos = copia.fotos.map { it.copy(arquivo = "../banco.db") }), "foto")
        recusa(copia.copy(rotas = copia.rotas.map { it.copy(emGravacao = true) }), "gravação")
        recusa(copia.copy(registros = copia.registros.map { it.copy(latitude = 91.0, longitude = 0.0) }), "globo")
    }

    @Test(expected = FormatoNovoDemaisException::class)
    fun formatoMaisNovoEhRecusado() {
        conferir(copia.copy(formato = FORMATO_ATUAL + 1))
    }

    @Test
    fun mesmaMotoNoAparelhoENoArquivoNaoPedeEscolha() {
        val resultado = aplicarLimiteDeMotos(copia, listOf(moto), limite = 1, motoQueFica = null)
        assertEquals(CopiaNoLimite(copia, emptyList()), resultado)
    }

    @Test
    fun duasMotosNoGratisSemEscolhaPedemAMoto() {
        val doAparelho = Moto(id = "a", apelido = "Do aparelho", marca = "Honda", modelo = "CG")
        try {
            aplicarLimiteDeMotos(copia, listOf(doAparelho), limite = 1, motoQueFica = null)
            fail("deveria pedir a escolha")
        } catch (e: EscolhaDeMotoNecessariaException) {
            assertEquals(listOf("a", "m"), e.candidatas.map { it.id })
        }
        // Premium (sem limite): as duas ficam.
        assertEquals(
            emptyList<String>(),
            aplicarLimiteDeMotos(copia, listOf(doAparelho), null, null).motosDoAparelhoAExcluir,
        )
    }

    @Test
    fun escolherAMotoDoArquivoExcluiADoAparelho() {
        val doAparelho = Moto(id = "a", apelido = "Do aparelho", marca = "Honda", modelo = "CG")
        val resultado = aplicarLimiteDeMotos(copia, listOf(doAparelho), limite = 1, motoQueFica = "m")
        assertEquals(copia, resultado.copia)
        assertEquals(listOf("a"), resultado.motosDoAparelhoAExcluir)
    }

    @Test
    fun escolherAMotoDoAparelhoDeixaADoArquivoInteiraDeFora() {
        val doAparelho = Moto(id = "a", apelido = "Do aparelho", marca = "Honda", modelo = "CG")
        val resultado = aplicarLimiteDeMotos(copia, listOf(doAparelho), limite = 1, motoQueFica = "a")
        val vazia = copia.copy(
            motos = emptyList(),
            leituras = emptyList(),
            registros = emptyList(),
            fotos = emptyList(),
            itensDoPlano = emptyList(),
            rotas = emptyList(),
        )
        assertEquals(vazia, resultado.copia)
        assertEquals(emptyList<String>(), resultado.motosDoAparelhoAExcluir)
    }
}
