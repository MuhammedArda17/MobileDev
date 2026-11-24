package com.example.mobiledev.ui.merchant

import androidx.annotation.StringRes
import com.example.mobiledev.data.model.LatLon
import com.example.mobiledev.data.model.StoreCategory

/** Alles wat het scherm "Mijn winkel" toont. Zit in de ViewModel -> overleeft rotatie. */
data class MyStoreUiState(
    val isLoading: Boolean = true,        // bestaande winkel aan het laden
    val name: String = "",
    val category: StoreCategory? = null,
    val pin: LatLon? = null,              // waar de handelaar getikt heeft
    val mapCenter: LatLon = DEFAULT_CENTER,
    val address: String = "",             // straat + huisnummer
    val postcode: String = "",
    val city: String = "",
    val isGeocoding: Boolean = false,     // adres aan het opzoeken
    val isSaving: Boolean = false,
    @StringRes val errorRes: Int? = null,
    @StringRes val infoRes: Int? = null,  // neutrale melding (geen fout), bv. "benaderde locatie"
    val isSaved: Boolean = false,
    // --- "Gebruik mijn locatie" ---
    val showLocationRationale: Boolean = false,      // uitleg tonen VOOR we de permissie vragen
    val isLocating: Boolean = false,                 // locatie aan het zoeken
    val locationPermanentlyDenied: Boolean = false   // knop "Open instellingen" tonen
) {
    companion object {
        val DEFAULT_CENTER = LatLon(51.2194, 4.4025) // Antwerpen
    }
}
