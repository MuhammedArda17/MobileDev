package com.example.mobiledev.data.repository

import com.example.mobiledev.data.model.Bag
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

sealed class BagError : Exception() {
    object NotLoggedIn : BagError()
    object NoStore : BagError()      // handelaar heeft nog geen winkel (H1)
    object Network : BagError()
    object Unknown : BagError()
}

/** Alle Firestore-code voor pakketten (collectie "bags"). */
class BagRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val bags get() = db.collection("bags")

    /** H3 + H4: nieuw pakket opslaan. Geeft het id van het nieuwe document terug. */
    suspend fun createBag(
        title: String,
        description: String,
        price: Double,
        originalValue: Double,
        quantity: Int,
        pickupStart: Timestamp,
        pickupEnd: Timestamp,
        photoBase64: String
    ): String {
        val uid = auth.currentUser?.uid ?: throw BagError.NotLoggedIn
        try {
            // 1) De winkel van deze handelaar opzoeken (één winkel per handelaar)
            val store = db.collection("stores")
                .whereEqualTo("ownerId", uid)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull() ?: throw BagError.NoStore

            // 2) Pakket samenstellen: storeName en location kopiëren we uit de winkel
            val bag = Bag(
                storeId = store.id,
                storeName = store.getString("name") ?: "",
                title = title,
                description = description,
                price = price,
                originalValue = originalValue,
                quantityTotal = quantity,
                quantityLeft = quantity,      // in het begin is er nog niets gereserveerd
                pickupStart = pickupStart,
                pickupEnd = pickupEnd,
                location = store.getGeoPoint("location"),
                active = true,
                photoBase64 = photoBase64,
                ownerId = uid
            )

            // 3) Opslaan: Firestore maakt zelf een uniek id
            return bags.add(bag).await().id
        } catch (e: CancellationException) {
            throw e // coroutine werd gestopt: gewoon doorgeven
        } catch (e: Exception) {
            throw mapError(e)
        }
    }

    /** H5: realtime alle pakketten van de ingelogde handelaar (nieuwste eerst). */
    fun observeMyBags(): Flow<List<Bag>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return bags
            .whereEqualTo("ownerId", uid)
            .snapshots() // Flow: geeft een nieuwe lijst bij elke wijziging in Firestore
            .map { snapshot ->
                snapshot.toObjects(Bag::class.java)
                    .sortedByDescending { it.pickupStart } // lokaal sorteren: geen index nodig
            }
    }

    /** K3 + K5: realtime alle actieve pakketten van alle winkels. */
    fun observeAvailableBags(): Flow<List<Bag>> =
        bags
            .whereEqualTo("active", true)
            .snapshots() // nieuwe lijst bij elke wijziging (bv. iemand reserveert het laatste pakket)
            .map { snapshot -> snapshot.toObjects(Bag::class.java) }

    private fun mapError(e: Exception): BagError = when {
        e is BagError -> e
        e is FirebaseNetworkException -> BagError.Network
        e is FirebaseFirestoreException &&
            e.code == FirebaseFirestoreException.Code.UNAVAILABLE -> BagError.Network
        else -> BagError.Unknown
    }
}
