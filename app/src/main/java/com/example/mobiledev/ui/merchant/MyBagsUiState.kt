package com.example.mobiledev.ui.merchant

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap

/** Eén rij in de lijst "Mijn pakketten", klaar om te tonen (foto al gedecodeerd). */
data class BagListItem(
    val id: String,
    val title: String,
    val price: Double,
    val originalValue: Double,
    val pickupStart: String,   // "17:00"
    val pickupEnd: String,     // "18:30"
    val quantityLeft: Int,
    val reservedCount: Int,    // quantityTotal - quantityLeft
    val active: Boolean,
    val photo: ImageBitmap?
)

data class MyBagsUiState(
    val isLoading: Boolean = true,
    val bags: List<BagListItem> = emptyList(),
    @StringRes val errorRes: Int? = null,
    val needsStore: Boolean = false // nieuwe handelaar zonder winkel -> eerst naar "Mijn winkel"
)
