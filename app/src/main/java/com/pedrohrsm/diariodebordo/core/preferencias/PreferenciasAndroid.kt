package com.pedrohrsm.diariodebordo.core.preferencias

import android.content.Context
import android.content.SharedPreferences
import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import com.pedrohrsm.diariodebordo.core.designsystem.theme.TemaDiario
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Singleton
class PreferenciasAndroid @Inject constructor(@ApplicationContext context: Context) : Preferencias {
    private val prefs: SharedPreferences = context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    override val ajuste: Flow<AjusteTema> = callbackFlow {
        trySend(ajusteSalvo())
        val ouvinte = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(ajusteSalvo()) }
        prefs.registerOnSharedPreferenceChangeListener(ouvinte)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(ouvinte) }
    }.distinctUntilChanged()

    // Tema gravado por uma versão que não o conhece mais volta ao de partida, sem derrubar a abertura.
    override fun ajusteSalvo(): AjusteTema = AjusteTema(
        tema = TemaDiario.entries.find { it.name == prefs.getString(TEMA, null) } ?: AjusteTema().tema,
        modoOficina = prefs.getBoolean(MODO_OFICINA, false),
    )

    override fun salvar(ajuste: AjusteTema) {
        prefs.edit().putString(TEMA, ajuste.tema.name).putBoolean(MODO_OFICINA, ajuste.modoOficina).apply()
    }

    private companion object {
        const val ARQUIVO = "preferencias"
        const val TEMA = "tema"
        const val MODO_OFICINA = "modo_oficina"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferenciasModule {
    @Binds
    abstract fun bindPreferencias(impl: PreferenciasAndroid): Preferencias
}
