package com.example.mobiledev.data.repository

import com.example.mobiledev.data.model.LatLon
import com.example.mobiledev.data.model.Store
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

sealed class StoreError : Exception() {
    object NotLoggedIn : StoreError()
    object Network : StoreError()
    object Unknown : StoreError()
}

/** Alle Firestore-code voor winkels (collectie "stores"). */
class StoreRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val stores get() = db.collection("stores")

    /** De winkel van de ingelogde handelaar, of null als hij er nog geen heeft. */
    suspend fun getMyStore(): Store? {
        val uid = auth.currentUser?.uid ?: throw StoreError.NotLoggedIn
        return try {
            stores.document(uid).get().await().toObject(Store::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw mapError(e)
        }
    }

    /** H1: winkel aanmaken of bijwerken. Document-id = uid -> één winkel per handelaar. */
    suspend fun saveMyStore(
        name: String,
        category: String,
        location: LatLon,
        address: String,
        postcode: String,
        city: String
    ) {
        val uid = auth.currentUser?.uid ?: throw StoreError.NotLoggedIn
        val store = Store(
            ownerId = uid,
            name = name,
            category = category,
            location = GeoPoint(location.lat, location.lon),
            address = address,
            postcode = postcode,
            city = city
        )
        try {
            stores.document(uid).set(store).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw mapError(e)
        }
    }

    private fun mapError(e: Exception): StoreError = when {
        e is StoreError -> e
        e is FirebaseNetworkException -> StoreError.Network
        e is FirebaseFirestoreException &&
            e.code == FirebaseFirestoreException.Code.UNAVAILABLE -> StoreError.Network
        else -> StoreError.Unknown
    }
}
