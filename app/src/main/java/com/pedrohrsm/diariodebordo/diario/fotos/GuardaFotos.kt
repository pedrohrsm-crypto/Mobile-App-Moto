package com.pedrohrsm.diariodebordo.diario.fotos

import com.pedrohrsm.diariodebordo.diario.dominio.ArquivoNaPasta

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

    /**
     * Os arquivos da pasta de fotos, com a data de modificação, para a varredura de órfãs (#94). Lança
     * `IOException` se a pasta não puder ser lida: pasta ilegível nunca vira lista vazia.
     */
    fun listar(): List<ArquivoNaPasta>
}
