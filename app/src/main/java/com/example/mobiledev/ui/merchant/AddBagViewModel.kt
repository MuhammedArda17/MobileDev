package com.example.mobiledev.ui.merchant

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiledev.R
import com.example.mobiledev.data.repository.BagError
import com.example.mobiledev.data.repository.BagRepository
import com.example.mobiledev.util.ImageUtils
import com.example.mobiledev.util.NetworkUtils
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

/**
 * ViewModel voor "Pakket aanmaken" (H3 + H4).
 * AndroidViewModel i.p.v. ViewModel: we hebben de Application nodig om de foto
 * te lezen (contentResolver) en om te checken of er internet is.
 */
class AddBagViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = BagRepository()

    private val _state = MutableStateFlow(AddBagUiState())
    val state: StateFlow<AddBagUiState> = _state.asStateFlow()

    // --- Invoervelden: elke wijziging komt hier binnen ---
    fun onTitleChange(value: String) = _state.update { it.copy(title = value, errorRes = null) }
    fun onDescriptionChange(value: String) = _state.update { it.copy(description = value, errorRes = null) }
    fun onPriceChange(value: String) = _state.update { it.copy(price = value, errorRes = null) }
    fun onOriginalValueChange(value: String) = _state.update { it.copy(originalValue = value, errorRes = null) }
    fun onQuantityChange(value: String) = _state.update { it.copy(quantity = value, errorRes = null) }
    fun onPickupStartChange(minutes: Int) = _state.update { it.copy(pickupStartMinutes = minutes, errorRes = null) }
    fun onPickupEndChange(minutes: Int) = _state.update { it.copy(pickupEndMinutes = minutes, errorRes = null) }

    /** H4: foto gekozen in de galerij -> verkleinen, comprimeren, Base64 (op de achtergrond). */
    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessingPhoto = true, errorRes = null) }

            val resolver = getApplication<Application>().contentResolver
            // Bestand lezen = I/O-werk
            val bytes = withContext(Dispatchers.IO) { ImageUtils.readBytes(resolver, uri) }
            // Verkleinen, comprimeren en preview maken = rekenwerk
            val result = withContext(Dispatchers.Default) {
                val base64 = bytes?.let { ImageUtils.compressToBase64(it) }
                base64?.let { it to ImageUtils.base64ToImageBitmap(it) }
            }

            if (result == null) {
                _state.update { it.copy(isProcessingPhoto = false, errorRes = R.string.error_photo_unreadable) }
            } else {
                _state.update {
                    it.copy(
                        isProcessingPhoto = false,
                        photoBase64 = result.first,
                        photoPreview = result.second
                    )
                }
            }
        }
    }

    /** H3: alles controleren en opslaan in Firestore. */
    fun save() {
        val s = _state.value
        if (s.isSaving || s.isProcessingPhoto) return

        val price = s.price.toPriceOrNull()
        val originalValue = s.originalValue.toPriceOrNull()
        val quantity = s.quantity.trim().toIntOrNull()

        // 1) Validatie: de eerste fout die we vinden tonen we
        val error = when {
            s.photoBase64 == null -> R.string.error_bag_photo_missing
            s.title.isBlank() -> R.string.error_bag_title_empty
            s.description.isBlank() -> R.string.error_bag_description_empty
            originalValue == null || originalValue <= 0 -> R.string.error_bag_original_value_invalid
            price == null || price <= 0 -> R.string.error_bag_price_invalid
            price >= originalValue -> R.string.error_bag_price_too_high
            quantity == null || quantity < 1 -> R.string.error_bag_quantity_invalid
            s.pickupStartMinutes == null || s.pickupEndMinutes == null -> R.string.error_bag_pickup_missing
            s.pickupEndMinutes <= s.pickupStartMinutes -> R.string.error_bag_pickup_order
            s.pickupEndMinutes <= nowInMinutes() -> R.string.error_bag_pickup_past
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(errorRes = error) }
            return
        }

        // 2) Geen internet? Meteen een duidelijke melding (G1)
        if (!NetworkUtils.isOnline(getApplication<Application>())) {
            _state.update { it.copy(errorRes = R.string.error_no_internet) }
            return
        }

        // 3) Opslaan. De !! zijn hier veilig: de validatie hierboven heeft alles gecontroleerd.
        _state.update { it.copy(isSaving = true, errorRes = null) }
        viewModelScope.launch {
            try {
                repo.createBag(
                    title = s.title.trim(),
                    description = s.description.trim(),
                    price = price!!,
                    originalValue = originalValue!!,
                    quantity = quantity!!,
                    pickupStart = todayAt(s.pickupStartMinutes!!),
                    pickupEnd = todayAt(s.pickupEndMinutes!!),
                    photoBase64 = s.photoBase64!!
                )
                _state.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: BagError) {
                _state.update { it.copy(isSaving = false, errorRes = e.toStringRes()) }
            }
        }
    }

    /** "4,50" of "4.50" -> 4.5 (in België typen we vaak een komma) */
    private fun String.toPriceOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

    private fun nowInMinutes(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

    /** Ophaalvenster is altijd vandaag: 1050 -> vandaag om 17:30 als Firestore Timestamp */
    private fun todayAt(minutes: Int): Timestamp {
        val dateTime = LocalDate.now().atTime(minutes / 60, minutes % 60)
        val instant = dateTime.atZone(ZoneId.systemDefault()).toInstant()
        return Timestamp(Date.from(instant))
    }

    private fun BagError.toStringRes(): Int = when (this) {
        BagError.NotLoggedIn -> R.string.error_not_logged_in
        BagError.NoStore -> R.string.error_no_store
        BagError.Network -> R.string.error_no_internet
        BagError.Unknown -> R.string.error_unknown
    }
}
