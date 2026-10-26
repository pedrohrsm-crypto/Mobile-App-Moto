package com.pedrohrsm.diariodebordo.diario.dominio

import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.numeros.Leitor

const val MAXIMO_FOTOS = 3

/** Lado maior da foto guardada, em pixels: legível na tela e leve no armazenamento. */
const val LADO_MAXIMO_FOTO = 1600

enum class CampoRegistro { TITULO, KM, VALOR, LITROS, FOTOS }

/** Campos inválidos do formulário do diário. Vazio quando pode salvar. Campos opcionais aceitam vazio. */
fun validarRegistro(
    tipo: TipoRegistro,
    titulo: String,
    kmTexto: String,
    valorTexto: String,
    litrosTexto: String,
    quantidadeFotos: Int,
): Set<CampoRegistro> = buildSet {
    if (titulo.isBlank()) add(CampoRegistro.TITULO)
    if (kmTexto.isNotBlank() && Leitor.km(kmTexto) == null) add(CampoRegistro.KM)
    if (valorTexto.isNotBlank() && Leitor.dinheiro(valorTexto) == null) add(CampoRegistro.VALOR)
    if (tipo == TipoRegistro.ABASTECIMENTO && litrosTexto.isNotBlank()) {
        val ml = Leitor.litros(litrosTexto)
        if (ml == null || ml == 0L) add(CampoRegistro.LITROS)
    }
    if (quantidadeFotos > MAXIMO_FOTOS) add(CampoRegistro.FOTOS)
}

/**
 * Fator de redução (potência de 2) para abrir a foto já pequena, sem estourar a memória
 * em aparelhos Android 8 com pouca RAM. O lado maior fica entre [ladoMaximo] e o dobro dele.
 */
fun fatorDeReducao(largura: Int, altura: Int, ladoMaximo: Int = LADO_MAXIMO_FOTO): Int {
    require(largura > 0 && altura > 0) { "Foto sem dimensão" }
    var fator = 1
    while (maxOf(largura, altura) / (fator * 2) >= ladoMaximo) fator *= 2
    return fator
}

/** Escala final para o lado maior caber em [ladoMaximo]; 1.0 quando já cabe. */
fun escalaFinal(largura: Int, altura: Int, ladoMaximo: Int = LADO_MAXIMO_FOTO): Double {
    val maior = maxOf(largura, altura)
    return if (maior <= ladoMaximo) 1.0 else ladoMaximo.toDouble() / maior
}
