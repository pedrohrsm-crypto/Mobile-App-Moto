package com.pedrohrsm.diariodebordo.garagem.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HodometroTest {

    @Test
    fun primeiraLeituraEhAceita() {
        assertEquals(ValidacaoLeitura.Aceita, validarLeitura(anteriorKm = null, novoKm = 0))
    }

    @Test
    fun hodometroNuncaAceitaValorMenorQueOUltimo() {
        assertEquals(ValidacaoLeitura.MenorQueAnterior(10_000), validarLeitura(10_000, 9_999))
    }

    @Test
    fun mesmoKmNaoViraLeituraNova() {
        assertEquals(ValidacaoLeitura.IgualAnterior(10_000), validarLeitura(10_000, 10_000))
    }

    @Test
    fun negativoEAbsurdoSaoRecusados() {
        assertEquals(ValidacaoLeitura.Negativa, validarLeitura(null, -1))
        assertEquals(ValidacaoLeitura.AcimaDoLimite, validarLeitura(null, KM_MAXIMO + 1))
    }

    @Test
    fun cadastroExigeCamposEKmValido() {
        val invalidos = validarMoto(" ", "Honda", "", "1850", "12,3", anoAtual = 2026)
        assertEquals(setOf(CampoMoto.APELIDO, CampoMoto.MODELO, CampoMoto.ANO, CampoMoto.KM), invalidos)
    }

    @Test
    fun edicaoNaoValidaKmEAnoEhOpcional() {
        assertTrue(validarMoto("Bandida", "Harley-Davidson", "883", "", kmTexto = null, anoAtual = 2026).isEmpty())
        assertTrue(validarMoto("Bandida", "Harley-Davidson", "883", "2027", kmTexto = "0", anoAtual = 2026).isEmpty())
    }

    @Test
    fun gratisPermiteUmaMotoEPremiumSemLimite() {
        assertTrue(podeCadastrarMoto(motosAtivas = 0))
        assertFalse(podeCadastrarMoto(motosAtivas = LIMITE_MOTOS_GRATIS))
        assertTrue(podeCadastrarMoto(motosAtivas = 5, limite = null))
    }
}

/** Edição e exclusão de leitura já lançada: o km fica estritamente entre as vizinhas pela data. */
class CorrecaoDeHodometroTest {

    private fun ponto(id: String, km: Int, dia: Int) = PontoDoHodometro(id, km, lidaEm = dia * 86_400_000L)

    private val a = ponto("a", 1_000, dia = 1)
    private val b = ponto("b", 2_000, dia = 2)
    private val c = ponto("c", 3_000, dia = 3)
    private val todas = listOf(c, a, b) // fora de ordem de propósito: a regra ordena pela data

    @Test
    fun editarAPrimeiraParaBaixoEhAceita() {
        assertEquals(ValidacaoEdicao.Aceita, validarEdicao(todas, "a", 0))
        assertEquals(ValidacaoEdicao.Aceita, validarEdicao(todas, "a", 1_999))
    }

    @Test
    fun editarAPrimeiraParaCimaDaSeguinteApontaASeguinte() {
        assertEquals(
            ValidacaoEdicao.ForaDoIntervalo(Vizinha.SEGUINTE, anterior = null, seguinte = b),
            validarEdicao(todas, "a", 2_000),
        )
    }

    @Test
    fun editarUmaDoMeioForaDoIntervaloApontaAVizinhaCerta() {
        assertEquals(
            ValidacaoEdicao.ForaDoIntervalo(Vizinha.ANTERIOR, anterior = a, seguinte = c),
            validarEdicao(todas, "b", 1_000),
        )
        assertEquals(
            ValidacaoEdicao.ForaDoIntervalo(Vizinha.ANTERIOR, anterior = a, seguinte = c),
            validarEdicao(todas, "b", 500),
        )
        assertEquals(
            ValidacaoEdicao.ForaDoIntervalo(Vizinha.SEGUINTE, anterior = a, seguinte = c),
            validarEdicao(todas, "b", 3_000),
        )
        assertEquals(ValidacaoEdicao.Aceita, validarEdicao(todas, "b", 1_001))
        assertEquals(ValidacaoEdicao.Aceita, validarEdicao(todas, "b", 2_999))
    }

    @Test
    fun editarAUltimaParaCimaEhAceitaEParaAbaixoDaAnteriorNao() {
        assertEquals(ValidacaoEdicao.Aceita, validarEdicao(todas, "c", 9_999_999))
        assertEquals(
            ValidacaoEdicao.ForaDoIntervalo(Vizinha.ANTERIOR, anterior = b, seguinte = null),
            validarEdicao(todas, "c", 2_000),
        )
    }

    @Test
    fun editarAUnicaLeituraSoTemOsLimitesAbsolutos() {
        val unica = listOf(a)
        assertEquals(ValidacaoEdicao.Aceita, validarEdicao(unica, "a", 5))
        assertEquals(ValidacaoEdicao.Negativa, validarEdicao(unica, "a", -1))
        assertEquals(ValidacaoEdicao.AcimaDoLimite, validarEdicao(unica, "a", KM_MAXIMO + 1))
        assertEquals(ValidacaoEdicao.NaoEncontrada, validarEdicao(unica, "x", 5))
    }

    @Test
    fun vizinhaEhPelaDataENaoPeloKm() {
        // Retroativo do diário: a data do meio tem o MAIOR km. Pela data a ordem é a(1.000), r(7.000), z(6.000);
        // pelo km seria a(1.000), z(6.000), r(7.000), e z ganharia r como seguinte em vez de anterior.
        val r = ponto("r", 7_000, dia = 2)
        val z = ponto("z", 6_000, dia = 3)
        val leituras = listOf(a, r, z)
        assertEquals(
            ValidacaoEdicao.ForaDoIntervalo(Vizinha.ANTERIOR, anterior = r, seguinte = null),
            validarEdicao(leituras, "z", 6_500),
        )
        assertEquals(ValidacaoEdicao.Aceita, validarEdicao(leituras, "z", 7_001))
        assertEquals(
            ValidacaoEdicao.ForaDoIntervalo(Vizinha.SEGUINTE, anterior = a, seguinte = z),
            validarEdicao(leituras, "r", 6_000),
        )
    }

    @Test
    fun leiturasDaMesmaDataDesempatamPeloKmEntaoPeloId() {
        // Mesma data: a ordem é pelo km (1.000 antes de 2.000), mesmo com o id "a" vindo antes do id "b".
        val menor = PontoDoHodometro("b", 1_000, lidaEm = 2 * 86_400_000L)
        val maior = PontoDoHodometro("a", 2_000, lidaEm = 2 * 86_400_000L)
        assertEquals(listOf(menor, maior), ordenarPelaData(listOf(maior, menor)))
        assertEquals(ValidacaoEdicao.Aceita, validarEdicao(listOf(maior, menor), "b", 1_500))
        assertEquals(
            ValidacaoEdicao.ForaDoIntervalo(Vizinha.SEGUINTE, anterior = null, seguinte = maior),
            validarEdicao(listOf(maior, menor), "b", 2_500),
        )
    }

    @Test
    fun kmIgualAoDeOutraLeituraEhRecusadoMesmoSemSerVizinha() {
        // Retroativo do diário: a(1.000, dia 1), r(7.000, dia 2), z(6.000, dia 3). Editar a para 6.000 cabe
        // abaixo da vizinha r, mas repete o km de z, que não é vizinha dela.
        val r = ponto("r", 7_000, dia = 2)
        val z = ponto("z", 6_000, dia = 3)
        assertEquals(
            ValidacaoEdicao.IgualAOutraLeitura(z),
            validarEdicao(listOf(a, r, z), "a", 6_000),
        )
        assertEquals(ValidacaoEdicao.Aceita, validarEdicao(listOf(a, r, z), "a", 6_001))
        // O próprio km não conta como repetido.
        assertEquals(ValidacaoEdicao.Aceita, validarEdicao(listOf(a, r, z), "a", a.km))
    }

    @Test
    fun apagarADoMeioDeixaAsVizinhasValidasEntreSi() {
        assertEquals(ValidacaoExclusao.Permitida, validarExclusao(todas, "b", motoTemRegistrosComKm = true))
        val restantes = todas.filter { it.id != "b" }
        // Cada vizinha continua aceitando o próprio km: nenhuma ficou fora do intervalo da outra.
        assertEquals(ValidacaoEdicao.Aceita, validarEdicao(restantes, "a", a.km))
        assertEquals(ValidacaoEdicao.Aceita, validarEdicao(restantes, "c", c.km))
    }

    @Test
    fun apagarAUnicaLeituraSoNegadoSeHaRegistrosComKm() {
        assertEquals(
            ValidacaoExclusao.UnicaComRegistrosDeKm,
            validarExclusao(listOf(a), "a", motoTemRegistrosComKm = true),
        )
        assertEquals(ValidacaoExclusao.Permitida, validarExclusao(listOf(a), "a", motoTemRegistrosComKm = false))
        assertEquals(ValidacaoExclusao.NaoEncontrada, validarExclusao(listOf(a), "x", motoTemRegistrosComKm = false))
    }
}
