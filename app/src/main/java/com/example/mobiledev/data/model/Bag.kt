package com.example.mobiledev.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

/**
 * Eén verrassingspakket, zoals het in Firestore staat (collectie "bags").
 * Alle velden hebben een standaardwaarde: Firestore heeft dat nodig om
 * een document terug om te zetten naar een Bag (toObjects).
 */
data class Bag(
    @DocumentId val id: String = "",   // = document-id, wordt niet als veld opgeslagen
    val storeId: String = "",
    val storeName: String = "",        // bewust gekopieerd uit de winkel
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val originalValue: Double = 0.0,
    val quantityTotal: Int = 0,
    val quantityLeft: Int = 0,
    val pickupStart: Timestamp? = null,
    val pickupEnd: Timestamp? = null,
    val location: GeoPoint? = null,    // bewust gekopieerd uit de winkel
    val active: Boolean = true,
    val photoBase64: String = "",
    val ownerId: String = ""           // extra veld: uid van de handelaar (voor security rules)
)
