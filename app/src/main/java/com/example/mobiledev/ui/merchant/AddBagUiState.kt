package com.example.mobiledev.ui.merchant

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Alles wat het scherm "Pakket aanmaken" nodig heeft.
 * Dit zit in de ViewModel, dus het overleeft een rotatie (G2).
 */
data class AddBagUiState(
    val title: String = "",
    val description: String = "",
    val price: String = "",            // als tekst, want de gebruiker typt het
    val originalValue: String = "",
    val quantity: String = "1",
    val pickupStartMinutes: Int? = null, // minuten sinds middernacht: 17:30 -> 1050
    val pickupEndMinutes: Int? = null,
    val photoBase64: String? = null,     // wat we opslaan in Firestore
    val photoPreview: ImageBitmap? = null, // wat we tonen (1x gedecodeerd)
    val isProcessingPhoto: Boolean = false,
    val isSaving: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val isSaved: Boolean = false         // true -> scherm sluit
)

/** 1050 -> "17:30" */
fun Int.toTimeText(): String = "%02d:%02d".format(this / 60, this % 60)
