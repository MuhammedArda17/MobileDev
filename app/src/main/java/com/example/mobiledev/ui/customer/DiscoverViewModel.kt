package com.example.mobiledev.ui.customer

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiledev.R
import com.example.mobiledev.data.local.AppDatabase
import com.example.mobiledev.data.model.Bag
import com.example.mobiledev.data.model.LatLon
import com.example.mobiledev.data.repository.BagRepository
import com.example.mobiledev.data.repository.GeocodingError
import com.example.mobiledev.data.repository.GeocodingRepository
import com.example.mobiledev.data.repository.LocationError
import com.example.mobiledev.data.repository.LocationRepository
import com.example.mobiledev.ui.location.LocationPermissionResult
import com.example.mobiledev.util.ImageUtils
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import org.osmdroid.util.GeoPoint as OsmGeoPoint

/** ViewModel voor het tabblad "Ontdekken" (K1, K2, K3, K5). */
class DiscoverViewModel(application: Application) : AndroidViewModel(application) {

    private val bagRepo = BagRepository()
    private val locationRepo = LocationRepository(application)
    private val geocodingRepo = GeocodingRepository(
        AppDatabase.getInstance(application).cachedAddressDao()
    )

    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    // Aparte flow zodat de lijst opnieuw berekend wordt (afstand + sortering) bij een nieuwe locatie
    private val location = MutableStateFlow<LatLon?>(null)
    private val photoCache = mutableMapOf<String, ImageBitmap?>()
    private var geocodeJob: Job? = null

    init {
        observeBags()
        // Al toestemming gekregen (bv. vorige keer)? Dan hoeven we niets te vragen.
        if (locationRepo.hasAnyPermission()) findMyLocation()
    }

    // ---------- Pakketten (K3 + K5) ----------

    private fun observeBags() {
        viewModelScope.launch {
            combine(bagRepo.observeAvailableBags(), location, ticker()) { bags, loc, now ->
                toItems(bags, loc, now)
            }
                .flowOn(Dispatchers.Default) // sorteren en foto's decoderen niet op de main thread
                .catch { _state.update { s -> s.copy(isLoading = false, errorRes = R.string.error_load_bags) } }
                .collect { items ->
                    _state.update { it.copy(isLoading = false, bags = items, errorRes = null) }
                }
        }
    }

    /** Elke 30 s een nieuw tijdstip: zo verdwijnt een pakket ook als het ophaalvenster voorbij is. */
    private fun ticker() = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000)
        }
    }

    private fun toItems(bags: List<Bag>, loc: LatLon?, now: Long): List<DiscoverBagItem> {
        val here = loc?.let { OsmGeoPoint(it.lat, it.lon) }
        return bags
            .filter { it.active && it.quantityLeft > 0 && (it.pickupEnd?.toDate()?.time ?: 0L) > now }
            .map { bag ->
                val bagLocation = bag.location
                val distanceKm = if (here != null && bagLocation != null) {
                    here.distanceToAsDouble(OsmGeoPoint(bagLocation.latitude, bagLocation.longitude)) / 1000.0
                } else null
                DiscoverBagItem(
                    id = bag.id,
                    storeName = bag.storeName,
                    title = bag.title,
                    price = bag.price,
                    originalValue = bag.originalValue,
                    pickupStart = bag.pickupStart.toTimeText(),
                    pickupEnd = bag.pickupEnd.toTimeText(),
                    quantityLeft = bag.quantityLeft,
                    distanceKm = distanceKm,
                    photo = photoCache.getOrPut(bag.photoBase64) {
                        ImageUtils.base64ToImageBitmap(bag.photoBase64)
                    }
                )
            }
            .sortedWith(compareBy(nullsLast<Double>()) { it.distanceKm }) // dichtstbij eerst
    }

    private fun Timestamp?.toTimeText(): String =
        this?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it.toDate()) } ?: "--:--"

    // ---------- Locatie (K1) ----------

    /** Knop "Gebruik mijn locatie": al toestemming? Meteen zoeken. Anders eerst uitleg tonen. */
    fun onUseMyLocationClick() {
        if (locationRepo.hasAnyPermission()) {
            findMyLocation()
        } else {
            _state.update { it.copy(showLocationRationale = true, locationMessageRes = null) }
        }
    }

    /** "Toestaan" in de uitleg: het scherm vraagt daarna zelf de permissie. */
    fun onRationaleAllow() = _state.update { it.copy(showLocationRationale = false) }

    /** "Annuleren": geen permissie vragen, de gebruiker kiest zelf een plek op de kaart. */
    fun onRationaleCancel() =
        _state.update { it.copy(showLocationRationale = false, isPickingOnMap = true) }

    /** Antwoord van Android op de permissievraag: alle vier de gevallen. */
    fun onLocationPermissionResult(result: LocationPermissionResult) {
        when (result) {
            LocationPermissionResult.PRECISE,
            LocationPermissionResult.APPROXIMATE -> findMyLocation()
            LocationPermissionResult.DENIED -> _state.update {
                it.copy(isPickingOnMap = true, locationMessageRes = R.string.error_location_denied_customer)
            }
            LocationPermissionResult.PERMANENTLY_DENIED -> _state.update {
                it.copy(
                    isPickingOnMap = true,
                    locationPermanentlyDenied = true,
                    locationMessageRes = R.string.error_location_denied_permanently_customer
                )
            }
        }
    }

    private fun findMyLocation() {
        _state.update {
            it.copy(isLocating = true, locationPermanentlyDenied = false, locationMessageRes = null)
        }
        viewModelScope.launch {
            try {
                val point = locationRepo.getCurrentLocation()
                _state.update { it.copy(isLocating = false, isPickingOnMap = false) }
                setLocation(point)
            } catch (e: LocationError) {
                val message = when (e) {
                    LocationError.NoPermission -> R.string.error_location_denied_customer
                    LocationError.Disabled -> R.string.error_location_disabled_customer
                    LocationError.Unavailable -> R.string.error_location_unavailable_customer
                }
                // Fallback: de gebruiker zet zelf een pin
                _state.update { it.copy(isLocating = false, isPickingOnMap = true, locationMessageRes = message) }
            }
        }
    }

    fun onChooseOnMapClick() = _state.update { it.copy(isPickingOnMap = true, locationMessageRes = null) }

    fun onPickingDone() = _state.update { it.copy(isPickingOnMap = false) }

    /** Tik op de kaart = nieuwe locatie. */
    fun onMapTap(point: LatLon) = setLocation(point)

    /** Locatie bewaren en de gemeente opzoeken (K2, met cache in Room). */
    private fun setLocation(point: LatLon) {
        location.value = point
        _state.update { it.copy(location = point, municipality = null) }

        geocodeJob?.cancel() // snel opnieuw tikken: vorige opzoeking stoppen
        geocodeJob = viewModelScope.launch {
            try {
                val result = geocodingRepo.reverse(point.lat, point.lon)
                _state.update { it.copy(municipality = result.city.ifBlank { null }) }
            } catch (e: GeocodingError) {
                val message = when (e) {
                    GeocodingError.Network -> R.string.error_no_internet
                    GeocodingError.NotFound -> R.string.error_municipality_not_found
                }
                _state.update { it.copy(locationMessageRes = message) }
            }
        }
    }
}