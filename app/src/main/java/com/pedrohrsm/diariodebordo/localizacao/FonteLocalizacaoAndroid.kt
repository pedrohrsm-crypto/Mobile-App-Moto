package com.pedrohrsm.diariodebordo.localizacao

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Fused Location do Google quando o aparelho tem Google Play Services; sem ele (aparelhos sem Google),
 * o LocationManager do Android com o provedor GPS. Decisão de 26/09/2026.
 * Toda chamada confere a permissão antes (PermissoesLocalizacao): o app funciona sem ela.
 */
@Singleton
class FonteLocalizacaoAndroid @Inject constructor(@ApplicationContext private val context: Context) :
    FonteLocalizacao {

    private val temGoogle: Boolean by lazy {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }

    @SuppressLint("MissingPermission") // conferida em exigirPermissao
    override suspend fun atual(): Ponto {
        exigirPermissao()
        if (temGoogle) {
            val cliente = LocationServices.getFusedLocationProviderClient(context)
            val local = cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .await()
            if (local != null) return local.paraPonto()
        }
        return withTimeoutOrNull(ESPERA_MAXIMA_MS) { atualizacoes(1_000, 0f).first() }
            ?: throw LocalIndisponivelException()
    }

    override fun atualizacoes(intervaloMs: Long, distanciaMinimaM: Float): Flow<Ponto> =
        if (temGoogle) fused(intervaloMs, distanciaMinimaM) else gps(intervaloMs, distanciaMinimaM)

    @SuppressLint("MissingPermission") // conferida em exigirPermissao
    private fun fused(intervaloMs: Long, distanciaMinimaM: Float): Flow<Ponto> = callbackFlow {
        exigirPermissao()
        val cliente = LocationServices.getFusedLocationProviderClient(context)
        val pedido = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervaloMs)
            .setMinUpdateDistanceMeters(distanciaMinimaM)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                resultado.locations.forEach { trySend(it.paraPonto()) }
            }
        }
        cliente.requestLocationUpdates(pedido, callback, Looper.getMainLooper())
            .addOnFailureListener { close(it) }
        awaitClose { cliente.removeLocationUpdates(callback) }
    }

    @SuppressLint("MissingPermission") // conferida em exigirPermissao
    private fun gps(intervaloMs: Long, distanciaMinimaM: Float): Flow<Ponto> = callbackFlow {
        exigirPermissao()
        val gerente = context.getSystemService(LocationManager::class.java)
        // Todos os métodos implementados: no Android 8 a interface ainda não tem métodos padrão.
        val ouvinte = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.paraPonto())
            }

            @Deprecated("Exigido pela interface até o Android 10")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }
        gerente.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            intervaloMs,
            distanciaMinimaM,
            ouvinte,
            Looper.getMainLooper(),
        )
        awaitClose { gerente.removeUpdates(ouvinte) }
    }

    private fun exigirPermissao() {
        if (!PermissoesLocalizacao.temAlguma(context)) throw SemPermissaoDeLocalizacaoException()
    }

    private fun Location.paraPonto() = Ponto(
        latitude = latitude,
        longitude = longitude,
        instante = time,
        precisaoM = if (hasAccuracy()) accuracy else null,
    )

    private companion object {
        const val ESPERA_MAXIMA_MS = 30_000L
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalizacaoModule {
    @Binds
    abstract fun bindFonte(impl: FonteLocalizacaoAndroid): FonteLocalizacao
}
