package com.pedrohrsm.diariodebordo.backup.dominio

import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.TipoOcorrencia
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import org.junit.Assert.assertEquals
import org.junit.Test

/** Risco #23, decisão R3: o local do registro só vai para o arquivo com a escolha da pessoa. */
class LocaisDosRegistrosTest {

    /**
     * Todo campo fora do padrão e latitude diferente da longitude: anular só um dos dois, trocar um pelo outro ou
     * mexer em qualquer outro campo muda o resultado.
     */
    private val registro = Registro(
        id = "r",
        motoId = "m",
        tipo = TipoRegistro.OCORRENCIA,
        titulo = "Barulho na corrente",
        descricao = "Na saída de casa",
        dataEm = 1_000,
        hodometroKm = 15_000,
        valorCentavos = 4_500,
        litrosMl = 12_000,
        tanqueCheio = true,
        ocorrencia = TipoOcorrencia.BARULHO,
        rotaId = "rota",
        latitude = -23.55,
        longitude = -46.63,
        criadoEm = 900,
        alteradoEm = 1_100,
        fusoHorario = "America/Sao_Paulo",
        excluidoEm = null,
        pendenteSync = true,
    )

    @Test
    fun semLocaisOArquivoLevaORegistroSemLatitudeELongitudeEComOResto() {
        assertEquals(registro.copy(latitude = null, longitude = null), registro.paraOArquivo(comLocais = false))
    }

    @Test
    fun comLocaisOArquivoLevaORegistroIgual() {
        assertEquals(registro, registro.paraOArquivo(comLocais = true))
    }

    /** Registro de outro aparelho, editado depois e exportado sem locais. */
    private val semLocal =
        registro.copy(titulo = "Barulho resolvido", alteradoEm = 2_000, latitude = null, longitude = null)
    private val novo = registro.copy(id = "novo", latitude = null, longitude = null)
    private val doAparelho = listOf(registro, registro.copy(id = "so-no-aparelho", latitude = 1.0, longitude = 2.0))

    @Test
    fun arquivoSemLocaisFicaComOLocalDoAparelhoEORegistroNovoChegaIgual() {
        assertEquals(
            listOf(semLocal.copy(latitude = -23.55, longitude = -46.63), novo),
            listOf(semLocal, novo).paraOAparelho(locaisOmitidos = true, doAparelho = doAparelho),
        )
    }

    @Test
    fun arquivoComLocaisChegaComoEsta() {
        val comOutroLocal = registro.copy(latitude = -22.9, longitude = -43.2)
        val chegando = listOf(comOutroLocal).paraOAparelho(locaisOmitidos = false, doAparelho = doAparelho)
        assertEquals(listOf(comOutroLocal), chegando)
    }
}
