package com.example.mobiledev.ui.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.app.ActivityCompat

/** De vier gevallen die we moeten afhandelen (zie opgave, "7 Permissies"). */
enum class LocationPermissionResult {
    PRECISE,            // precieze locatie toegestaan
    APPROXIMATE,        // enkel benaderde locatie
    DENIED,             // geweigerd (we mogen later opnieuw vragen)
    PERMANENTLY_DENIED  // definitief geweigerd -> enkel nog via de app-instellingen
}

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

/**
 * Geeft een functie terug die de locatiepermissie vraagt (FINE + COARSE samen).
 * Het resultaat komt binnen via [onResult].
 *
 * Dit moet in een composable staan: de Activity Result API hoort bij de UI.
 * Wat er met het resultaat gebeurt, beslist de ViewModel.
 * Herbruikbaar voor de klant (K1).
 */
@Composable
fun rememberLocationPermissionRequest(onResult: (LocationPermissionResult) -> Unit): () -> Unit {
    val activity = LocalActivity.current
    val currentOnResult by rememberUpdatedState(onResult)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val result = when {
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true -> LocationPermissionResult.PRECISE
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> LocationPermissionResult.APPROXIMATE
            // Geweigerd én Android wil geen uitleg meer tonen -> definitief geweigerd
            activity != null && LOCATION_PERMISSIONS.none {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            } -> LocationPermissionResult.PERMANENTLY_DENIED
            else -> LocationPermissionResult.DENIED
        }
        currentOnResult(result)
    }

    return { launcher.launch(LOCATION_PERMISSIONS) }
}

/** Opent de instellingen van onze app, zodat de gebruiker de permissie zelf kan aanzetten. */
fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
