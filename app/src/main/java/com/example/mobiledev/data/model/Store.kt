package com.example.mobiledev.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

/**
 * Eén winkel, zoals hij in Firestore staat (collectie "stores").
 * Het document-id is de uid van de handelaar: zo heeft elke handelaar
 * automatisch maximaal één winkel.
 */
data class Store(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val category: String = "",     // waarde uit StoreCategory, bv. "bakker"
    val location: GeoPoint? = null,
    val address: String = "",      // straat + huisnummer, bv. "Meir 1"
    val postcode: String = "",     // extra veld
    val city: String = ""
)

/** De categorieën waaruit een handelaar kan kiezen (ook handig voor de filter in K6). */
enum class StoreCategory(val value: String) {
    BAKER("bakker"),
    SUPERMARKET("supermarkt"),
    RESTAURANT("restaurant"),
    BUTCHER("slager"),
    GREENGROCER("groenten"),
    OTHER("andere");

    companion object {
        fun fromValue(value: String?): StoreCategory? = entries.firstOrNull { it.value == value }
    }
}

/** Eenvoudig punt op de kaart (los van Firestore en OSMDroid). */
data class LatLon(val lat: Double, val lon: Double)
