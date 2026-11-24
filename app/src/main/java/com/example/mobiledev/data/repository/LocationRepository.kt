package com.example.mobiledev.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.mobiledev.data.model.LatLon
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

sealed class LocationError : Exception() {
    object NoPermission : LocationError() // gebruiker heeft geen toestemming gegeven
    object Disabled : LocationError()     // locatie staat uit op het toestel
    object Unavailable : LocationError()  // geen locatie gevonden (bv. binnen, geen GPS-signaal)
}

/**
 * Huidige locatie van het toestel opvragen.
 * We gebruiken de LocationManager van Android zelf (via LocationManagerCompat),
 * zodat er geen extra bibliotheek (Google Play Services) nodig is.
 */
class LocationRepository(private val context: Context) {

    private val manager: LocationManager? = context.getSystemService(LocationManager::class.java)

    /** Precieze locatie toegestaan? */
    fun hasFinePermission(): Boolean = isGranted(Manifest.permission.ACCESS_FINE_LOCATION)

    /** Minstens een benaderde locatie toegestaan? */
    fun hasAnyPermission(): Boolean =
        hasFinePermission() || isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

    /**
     * Geeft de huidige locatie terug. We wachten max. 10 seconden op een verse locatie;
     * lukt dat niet, dan gebruiken we de laatst gekende locatie.
     */
    @SuppressLint("MissingPermission") // we controleren de permissie hieronder zelf
    suspend fun getCurrentLocation(): LatLon {
        if (!hasAnyPermission()) throw LocationError.NoPermission
        val manager = manager ?: throw LocationError.Unavailable
        if (!LocationManagerCompat.isLocationEnabled(manager)) throw LocationError.Disabled

        val provider = chooseProvider(manager) ?: throw LocationError.Disabled

        val location = withTimeoutOrNull(10_000) { requestFreshLocation(manager, provider) }
            ?: manager.getLastKnownLocation(provider)
            ?: throw LocationError.Unavailable

        return LatLon(location.latitude, location.longitude)
    }

    /** Welke bron gebruiken we? "fused" (Android 12+) > GPS (enkel met precieze permissie) > netwerk. */
    private fun chooseProvider(manager: LocationManager): String? {
        val enabled = manager.getProviders(true)
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                LocationManager.FUSED_PROVIDER in enabled -> LocationManager.FUSED_PROVIDER
            hasFinePermission() && LocationManager.GPS_PROVIDER in enabled -> LocationManager.GPS_PROVIDER
            LocationManager.NETWORK_PROVIDER in enabled -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
    }

    /** Callback van Android omzetten naar een suspend-functie (coroutine). */
    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() } // bv. bij timeout of scherm weg
            LocationManagerCompat.getCurrentLocation(
                manager,
                provider,
                signal,
                ContextCompat.getMainExecutor(context)
            ) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
        }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
