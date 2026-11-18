package com.example.mobiledev.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room-tabel met adressen die we al eens opgezocht hebben bij Nominatim.
 * key = afgeronde coördinaten (4 decimalen ≈ 10 m), bv. "51.2194,4.4025".
 * Zo vragen we hetzelfde adres nooit twee keer op (gebruiksvoorwaarden Nominatim).
 */
@Entity(tableName = "cached_addresses")
data class CachedAddress(
    @PrimaryKey val key: String,
    val displayName: String,
    val street: String,    // extra veld: straat + huisnummer
    val postcode: String,  // extra veld
    val city: String,
    val fetchedAt: Long
)
