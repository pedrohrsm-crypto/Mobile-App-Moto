package com.pedrohrsm.diariodebordo.rota.data

import android.content.Context
import android.content.SharedPreferences
import com.pedrohrsm.diariodebordo.rota.dominio.CorteDasPontas
import com.pedrohrsm.diariodebordo.rota.dominio.ProtecaoDasRotas
import com.pedrohrsm.diariodebordo.rota.dominio.ZonaDePrivacidade
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Arquivo de preferências próprio, separado do tema. Toda escrita usa `commit()` e confere o resultado. As mensagens
 * de erro nunca levam o conteúdo: coordenada de zona não vai para log nem para relatório de falha.
 */
@Singleton
class PrivacidadeDasRotasAndroid @Inject constructor(@ApplicationContext context: Context) : PrivacidadeDasRotas {
    private val prefs: SharedPreferences = context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
    private val escrita = Mutex()

    override val protecao: Flow<ProtecaoDasRotas> = callbackFlow {
        send(ler())
        val ouvinte = SharedPreferences.OnSharedPreferenceChangeListener { _, chave ->
            if (chave != SEGREDO) {
                try {
                    trySend(ler())
                } catch (e: IllegalStateException) {
                    close(e)
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(ouvinte)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(ouvinte) }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    override suspend fun atual(): ProtecaoDasRotas = withContext(Dispatchers.IO) { ler() }

    override suspend fun segredo(): ByteArray = withContext(Dispatchers.IO) {
        escrita.withLock {
            prefs.getString(SEGREDO, null)?.let { return@withLock decodificarSegredo(it) }
            val novo = ByteArray(TAMANHO_DO_SEGREDO).also { SecureRandom().nextBytes(it) }
            gravar { putString(SEGREDO, Base64.getEncoder().encodeToString(novo)) }
            novo
        }
    }

    override suspend fun salvarCorte(corte: CorteDasPontas) = withContext(Dispatchers.IO) {
        escrita.withLock { gravar { putString(CORTE, corte.name) } }
    }

    override suspend fun salvarZona(zona: ZonaDePrivacidade) = withContext(Dispatchers.IO) {
        escrita.withLock {
            val zonas = lerZonas()
            val novas = if (zonas.any { it.id == zona.id }) {
                zonas.map { if (it.id == zona.id) zona else it }
            } else {
                zonas + zona
            }
            gravarZonas(novas)
        }
    }

    override suspend fun excluirZona(id: String) = withContext(Dispatchers.IO) {
        escrita.withLock {
            val zonas = lerZonas()
            val restantes = zonas.filter { it.id != id }
            check(restantes.size == zonas.size - 1) { "Zona de privacidade não encontrada" }
            gravarZonas(restantes)
        }
    }

    // Corte gravado por uma versão que não o conhece mais volta ao padrão (1 km), nunca a "desligado".
    private fun ler(): ProtecaoDasRotas = ProtecaoDasRotas(
        corte = prefs.getString(CORTE, null)
            ?.let { nome -> CorteDasPontas.entries.find { it.name == nome } }
            ?: CorteDasPontas.PADRAO,
        zonas = lerZonas(),
    )

    private fun lerZonas(): List<ZonaDePrivacidade> {
        val texto = prefs.getString(ZONAS, null) ?: return emptyList()
        return try {
            json.decodeFromString(SERIALIZADOR, texto)
        } catch (e: SerializationException) {
            // Sem a causa: a mensagem do kotlinx.serialization traz um pedaço do JSON, com as coordenadas.
            throw ZonasIlegiveisException()
        } catch (e: IllegalArgumentException) {
            throw ZonasIlegiveisException()
        }
    }

    private fun gravarZonas(zonas: List<ZonaDePrivacidade>) =
        gravar { putString(ZONAS, json.encodeToString(SERIALIZADOR, zonas)) }

    private fun gravar(bloco: SharedPreferences.Editor.() -> Unit) {
        check(prefs.edit().apply(bloco).commit()) { "Não foi possível gravar a privacidade das rotas" }
    }

    private fun decodificarSegredo(texto: String): ByteArray = try {
        Base64.getDecoder().decode(texto).also { check(it.size == TAMANHO_DO_SEGREDO) }
    } catch (e: IllegalArgumentException) {
        throw IllegalStateException("Segredo do corte ilegível")
    }

    private companion object {
        const val ARQUIVO = "privacidade_rotas"
        const val CORTE = "corte"
        const val ZONAS = "zonas"
        const val SEGREDO = "segredo_do_corte"
        const val TAMANHO_DO_SEGREDO = 32
        val SERIALIZADOR = ListSerializer(ZonaDePrivacidade.serializer())
        val json = Json { ignoreUnknownKeys = true }
    }
}

/**
 * As zonas gravadas não puderam ser lidas. A exportação para aqui: sem as zonas, sairia sem proteção. Sem causa
 * encadeada, para nenhum pedaço do dado gravado chegar a um relatório de falha.
 */
class ZonasIlegiveisException : IllegalStateException("Zonas de privacidade ilegíveis")

@Module
@InstallIn(SingletonComponent::class)
abstract class PrivacidadeDasRotasModule {
    @Binds
    abstract fun bindPrivacidadeDasRotas(impl: PrivacidadeDasRotasAndroid): PrivacidadeDasRotas
}
