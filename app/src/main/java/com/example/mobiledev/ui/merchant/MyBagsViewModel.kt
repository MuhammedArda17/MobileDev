package com.example.mobiledev.ui.merchant

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiledev.R
import com.example.mobiledev.data.model.Bag
import com.example.mobiledev.data.repository.BagRepository
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
    private val repo: BagRepository = BagRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(MyBagsUiState())
    val state: StateFlow<MyBagsUiState> = _state.asStateFlow()

    // Foto's die we al gedecodeerd hebben: niet bij elke update opnieuw doen
    private val photoCache = mutableMapOf<String, ImageBitmap?>()

    init {
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
