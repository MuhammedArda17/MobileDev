package com.example.mobiledev.data.repository

import com.example.mobiledev.data.local.CachedAddress
import com.example.mobiledev.data.local.CachedAddressDao
import com.example.mobiledev.data.remote.NominatimApi
import com.example.mobiledev.data.remote.NominatimClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.Locale

sealed class GeocodingError : Exception() {
    object Network : GeocodingError()
    object NotFound : GeocodingError()
}

/**
 * Reverse geocoding (H2): coördinaten -> adres.
 * 1) Eerst kijken in de Room-cache.
 * 2) Niet gevonden? Dan Nominatim vragen (max 1 aanvraag per seconde) en het resultaat bewaren.
 */
class GeocodingRepository(
    private val dao: CachedAddressDao,
    private val api: NominatimApi = NominatimClient.api
) {

    suspend fun reverse(lat: Double, lon: Double): CachedAddress {
        // Afronden op 4 decimalen (≈ 10 m). Locale.US zodat er een punt staat en geen komma.
        val key = "%.4f,%.4f".format(Locale.US, lat, lon)

        // 1) Cache
        dao.get(key)?.let { return it }

        // 2) Nominatim
        val response = try {
            rateLimited { api.reverse(lat, lon) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw GeocodingError.Network // geen internet
        } catch (e: Exception) {
            throw GeocodingError.NotFound // bv. fout van de server
        }

        val address = response.address ?: throw GeocodingError.NotFound
        val result = CachedAddress(
            key = key,
            displayName = response.displayName ?: "",
            street = listOfNotNull(address.road, address.houseNumber).joinToString(" "),
            postcode = address.postcode ?: "",
            city = address.city ?: address.town ?: address.village ?: address.municipality ?: "",
            fetchedAt = System.currentTimeMillis()
        )
        dao.insert(result)
        return result
    }

    /** Zorgt dat er nooit meer dan 1 aanvraag per seconde naar Nominatim gaat. */
    private suspend fun <T> rateLimited(block: suspend () -> T): T = mutex.withLock {
        val waitMs = 1000 - (System.currentTimeMillis() - lastRequestAt)
        if (waitMs > 0) delay(waitMs)
        try {
            block()
        } finally {
            lastRequestAt = System.currentTimeMillis()
        }
    }

    companion object {
        // companion = gedeeld door de hele app, ook als er meerdere repositories zijn
        private val mutex = Mutex()
        private var lastRequestAt = 0L
    }
}
