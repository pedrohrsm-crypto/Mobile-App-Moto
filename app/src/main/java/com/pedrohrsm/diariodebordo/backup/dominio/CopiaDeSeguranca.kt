package com.pedrohrsm.diariodebordo.backup.dominio

import com.pedrohrsm.diariodebordo.diario.data.FotoRegistro
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.garagem.data.LeituraHodometro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import com.pedrohrsm.diariodebordo.plano.data.ItemDoPlano
import com.pedrohrsm.diariodebordo.rota.data.Rota
import kotlinx.serialization.Serializable

/** Versão do formato do arquivo. Sobe quando o JSON muda; a importação converte das versões anteriores. */
const val FORMATO_ATUAL = 1

/**
 * Conteúdo do arquivo de backup (diario.json dentro do .zip), com as tabelas da versão 1 do banco. Leva só o que
 * está ativo: itens excluídos não saem do aparelho, e `pontos_rota` nunca sai, porque os pontos brutos só existem
 * durante a gravação e são apagados ao salvar a rota (decisão #17). As fotos vão ao lado, em fotos/.
 */
@Serializable
data class CopiaDeSeguranca(
    val formato: Int = FORMATO_ATUAL,
    val geradoEm: Long,
    val fusoHorario: String,
    val motos: List<Moto> = emptyList(),
    val leituras: List<LeituraHodometro> = emptyList(),
    val registros: List<Registro> = emptyList(),
    val fotos: List<FotoRegistro> = emptyList(),
    val itensDoPlano: List<ItemDoPlano> = emptyList(),
    val rotas: List<Rota> = emptyList(),
)

/** O arquivo veio de uma versão mais nova do app: atualizar o app antes de importar. */
class FormatoNovoDemaisException(val formato: Int) :
    IllegalStateException("Arquivo no formato $formato; este app lê até o $FORMATO_ATUAL")

/** Não é um backup do Diário de Bordo, ou o arquivo está corrompido ou foi alterado. */
class ArquivoInvalidoException(motivo: String, causa: Throwable? = null) : IllegalArgumentException(motivo, causa)

/** As fotos passariam do que esta importação pode gravar no aparelho. */
class EspacoInsuficienteException(val limiteBytes: Long) :
    IllegalStateException("As fotos do arquivo passam de $limiteBytes bytes")
