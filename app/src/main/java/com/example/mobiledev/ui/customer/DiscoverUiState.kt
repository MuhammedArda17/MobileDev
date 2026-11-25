package com.example.mobiledev.ui.customer

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import com.example.mobiledev.data.model.LatLon

/** Eén pakket in de lijst "Ontdekken", klaar om te tonen. */
data class DiscoverBagItem(
    val id: String,
    val storeName: String,
    val title: String,
    val price: Double,
    val originalValue: Double,
    val pickupStart: String,
    val pickupEnd: String,
    val quantityLeft: Int,
    val distanceKm: Double?,   // null zolang we geen locatie hebben
    val photo: ImageBitmap?
)

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val bags: List<DiscoverBagItem> = emptyList(),
    val location: LatLon? = null,          // huidige of gekozen locatie
    val municipality: String? = null,      // K2: komt van reverse geocoding
    val isLocating: Boolean = false,
    val showLocationRationale: Boolean = false,
    val locationPermanentlyDenied: Boolean = false,
    val isPickingOnMap: Boolean = false,   // K1: pin zetten als fallback
    @StringRes val errorRes: Int? = null,          // fout bij het laden van pakketten
    @StringRes val locationMessageRes: Int? = null // melding over de locatie
) {
    companion object {
        val DEFAULT_CENTER = LatLon(51.2194, 4.4025) // Antwerpen
    }
}