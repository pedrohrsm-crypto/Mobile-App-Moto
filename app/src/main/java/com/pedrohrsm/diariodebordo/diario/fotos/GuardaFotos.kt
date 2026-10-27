package com.pedrohrsm.diariodebordo.diario.fotos

/**
 * Guarda as fotos do diário no armazenamento interno. Interface para os ViewModels serem
 * testados na JVM; no app vale a [ArmazenamentoFotos].
 */
interface GuardaFotos {
    /** Copia a foto escolhida (uri em texto), já reduzida e sem EXIF. Devolve o nome do arquivo. */
    suspend fun importar(uri: String): String

    /**
     * Apaga o arquivo. Devolve false se não conseguiu: o arquivo fica órfão no armazenamento
     * interno, sem efeito nos dados, e quem chama segue em frente.
     */
    fun apagar(arquivo: String): Boolean
}
