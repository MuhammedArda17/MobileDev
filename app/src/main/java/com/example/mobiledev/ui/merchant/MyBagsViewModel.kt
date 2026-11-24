package com.example.mobiledev.ui.merchant

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiledev.R
import com.example.mobiledev.data.model.Bag
import com.example.mobiledev.data.repository.BagRepository
import com.example.mobiledev.data.repository.StoreError
import com.example.mobiledev.data.repository.StoreRepository
import com.example.mobiledev.util.ImageUtils
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/** ViewModel voor "Mijn pakketten" (H5): realtime lijst van de eigen pakketten. */
class MyBagsViewModel(
    private val repo: BagRepository = BagRepository(),
    private val storeRepo: StoreRepository = StoreRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(MyBagsUiState())
    val state: StateFlow<MyBagsUiState> = _state.asStateFlow()

    // Foto's die we al gedecodeerd hebben: niet bij elke update opnieuw doen
    private val photoCache = mutableMapOf<String, ImageBitmap?>()

    init {
        checkStore()
        observeBags()
    }

    /**
     * H1: een handelaar zonder winkel kan nog geen pakketten aanbieden.
     * Daarom sturen we hem bij het openen meteen naar "Mijn winkel".
     * Dit gebeurt maar één keer per ViewModel: komt hij terug zonder op te slaan,
     * dan blijft hij gewoon op "Mijn pakketten".
     */
    private fun checkStore() = viewModelScope.launch {
        try {
            if (storeRepo.getMyStore() == null) {
                _state.update { it.copy(needsStore = true) }
            }
        } catch (e: StoreError) {
            // Geen internet of niet ingelogd: niet doorsturen, de lijst toont zelf een melding
        }
    }

    /** Het scherm heeft de doorverwijzing uitgevoerd. */
    fun onStoreRedirectHandled() = _state.update { it.copy(needsStore = false) }

    private fun observeBags() {
        viewModelScope.launch {
            repo.observeMyBags()
                .map { bags -> bags.map { it.toListItem() } }
                .flowOn(Dispatchers.Default) // decoderen op een achtergrondthread
                .catch { _state.update { s -> s.copy(isLoading = false, errorRes = R.string.error_load_bags) } }
                .collect { items ->
                    _state.update { it.copy(isLoading = false, bags = items, errorRes = null) }
                }
        }
    }

    private fun Bag.toListItem() = BagListItem(
        id = id,
        title = title,
        price = price,
        originalValue = originalValue,
        pickupStart = pickupStart.toTimeText(),
        pickupEnd = pickupEnd.toTimeText(),
        quantityLeft = quantityLeft,
        reservedCount = quantityTotal - quantityLeft,
        active = active,
        photo = photoCache.getOrPut(photoBase64) { ImageUtils.base64ToImageBitmap(photoBase64) }
    )

    private fun Timestamp?.toTimeText(): String =
        this?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it.toDate()) } ?: "--:--"
}
