package com.pedrohrsm.diariodebordo.rota.data

import com.pedrohrsm.diariodebordo.core.database.Transacao
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.diario.data.RegistroRepositorio
import com.pedrohrsm.diariodebordo.diario.data.TipoRegistro
import com.pedrohrsm.diariodebordo.localizacao.Ponto
import com.pedrohrsm.diariodebordo.rota.dominio.Polilinha
import com.pedrohrsm.diariodebordo.rota.dominio.distanciaTotalM
import com.pedrohrsm.diariodebordo.rota.dominio.semRuido
import com.pedrohrsm.diariodebordo.rota.dominio.simplificar
import java.time.Clock
import javax.inject.Inject
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.Flow

/** Único lugar que escreve rotas. Uma gravação por vez; ao salvar, os pontos brutos são apagados. */
class RotaRepositorio @Inject constructor(
    private val rotas: RotaDao,
    private val pontos: PontoDeRotaDao,
    private val registros: RegistroRepositorio,
    private val transacao: Transacao,
    private val relogio: Clock,
) {
    fun observarSalvasDaMoto(motoId: String): Flow<List<Rota>> = rotas.observarSalvasDaMoto(motoId)

    fun observarEmGravacao(): Flow<Rota?> = rotas.observarEmGravacao()

    fun observar(id: String): Flow<Rota?> = rotas.observar(id)

    fun observarQuantidadeDePontos(rotaId: String): Flow<Int> = pontos.observarQuantidade(rotaId)

    suspend fun emGravacao(): Rota? = rotas.emGravacao()

    /** Começa uma rota. Se já houver uma gravando, devolve essa (o serviço reiniciou, por exemplo). */
    suspend fun iniciar(motoId: String): Rota = transacao.executar {
        rotas.emGravacao() ?: run {
            val agora = relogio.millis()
            Rota(
                motoId = motoId,
                iniciadaEm = agora,
                criadoEm = agora,
                alteradoEm = agora,
                fusoHorario = relogio.zone.id,
            ).also { rotas.inserir(it) }
        }
    }

    suspend fun adicionarPonto(rotaId: String, ponto: Ponto) {
        val agora = relogio.millis()
        pontos.inserir(
            PontoDeRota(
                rotaId = rotaId,
                latitude = ponto.latitude,
                longitude = ponto.longitude,
                instante = ponto.instante,
                precisaoM = ponto.precisaoM,
                criadoEm = agora,
                alteradoEm = agora,
                fusoHorario = relogio.zone.id,
            ),
        )
    }

    /**
     * Encerra a gravação: calcula distância e duração no aparelho, guarda só o traçado simplificado,
     * apaga os pontos brutos (decisão #17, conferindo as linhas) e cria o registro do rolê no diário com
     * [tituloDoRole] (texto de strings.xml, passado por quem chama), tudo numa transação.
     */
    suspend fun finalizar(rotaId: String, tituloDoRole: String): Rota = transacao.executar {
        val rota = checkNotNull(rotas.buscar(rotaId)) { "Rota $rotaId não existe" }
        check(rota.emGravacao) { "Rota $rotaId já foi salva" }
        val brutos = pontos.daRota(rotaId).map { Ponto(it.latitude, it.longitude, it.instante, it.precisaoM) }
        val limpos = semRuido(brutos)
        val agora = relogio.millis()
        val fim = limpos.lastOrNull()?.instante ?: agora
        val salva = rota.copy(
            terminadaEm = fim,
            distanciaM = distanciaTotalM(limpos).roundToLong(),
            duracaoS = ((fim - rota.iniciadaEm) / 1000).coerceAtLeast(0),
            polilinha = Polilinha.codificar(simplificar(limpos)),
            emGravacao = false,
            alteradoEm = agora,
        )
        check(rotas.atualizar(salva) == 1) { "Rota $rotaId não foi salva" }
        val apagados = pontos.apagarDaRota(rotaId)
        check(apagados == brutos.size) { "Rota $rotaId: ${brutos.size} pontos brutos, $apagados apagados" }
        registros.salvar(
            Registro(
                motoId = rota.motoId,
                tipo = TipoRegistro.ROLE,
                titulo = tituloDoRole,
                dataEm = rota.iniciadaEm,
                rotaId = rotaId,
            ),
            emptyList(),
        )
        salva
    }

    /** Joga fora uma gravação sem salvar: os pontos brutos saem todos, conferidos pela contagem lida antes. */
    suspend fun descartar(rotaId: String) = transacao.executar {
        val antes = pontos.quantidade(rotaId)
        val apagados = pontos.apagarDaRota(rotaId)
        check(apagados == antes) { "Rota $rotaId: $antes pontos brutos, $apagados apagados" }
        check(rotas.marcarExcluido(rotaId, relogio.millis()) == 1) { "Rota $rotaId não foi descartada" }
    }

    /** Exclusão lógica; o traçado sai junto (risco #23): a rota excluída não guarda mais o trajeto. */
    suspend fun excluir(rotaId: String) {
        check(rotas.marcarExcluido(rotaId, relogio.millis()) == 1) { "Rota $rotaId não foi excluída" }
    }
}
