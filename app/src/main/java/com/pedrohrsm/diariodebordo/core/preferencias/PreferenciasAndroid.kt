package com.pedrohrsm.diariodebordo.core.preferencias

import android.content.Context
import android.content.SharedPreferences
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

    override val motoSelecionada: Flow<String?> = observar { it.getString(MOTO_SELECIONADA, null) }

    override fun selecionarMoto(id: String?) {
        prefs.edit().putString(MOTO_SELECIONADA, id).apply()
    }

    private fun <T> observar(ler: (SharedPreferences) -> T): Flow<T> = callbackFlow {
        trySend(ler(prefs))
        val ouvinte = SharedPreferences.OnSharedPreferenceChangeListener { p, _ -> trySend(ler(p)) }
        prefs.registerOnSharedPreferenceChangeListener(ouvinte)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(ouvinte) }
    }.distinctUntilChanged()

    private companion object {
        const val ARQUIVO = "preferencias"
        const val MOTO_SELECIONADA = "moto_selecionada"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferenciasModule {
    @Binds
    abstract fun bindPreferencias(impl: PreferenciasAndroid): Preferencias
}
