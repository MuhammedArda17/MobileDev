package com.example.mobiledev.ui.merchant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiledev.R
import com.example.mobiledev.data.local.AppDatabase
import com.example.mobiledev.data.model.LatLon
import com.example.mobiledev.data.model.StoreCategory
import com.example.mobiledev.data.repository.GeocodingError
import com.example.mobiledev.data.repository.GeocodingRepository
import com.example.mobiledev.data.repository.LocationError
import com.example.mobiledev.data.repository.LocationRepository
import com.example.mobiledev.data.repository.StoreError
import com.example.mobiledev.data.repository.StoreRepository
import com.example.mobiledev.ui.location.LocationPermissionResult
import com.example.mobiledev.util.NetworkUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** ViewModel voor "Mijn winkel" (H1 + H2). */
class MyStoreViewModel(application: Application) : AndroidViewModel(application) {

    private val storeRepo = StoreRepository()
    private val geocodingRepo = GeocodingRepository(
        AppDatabase.getInstance(application).cachedAddressDao()
    )

    private val locationRepo = LocationRepository(application)

    private val _state = MutableStateFlow(MyStoreUiState())
    val state: StateFlow<MyStoreUiState> = _state.asStateFlow()

    private var geocodeJob: Job? = null

    init {
        loadStore()
    }

    /** Heeft de handelaar al een winkel? Dan vullen we het formulier in (aanpassen). */
    private fun loadStore() = viewModelScope.launch {
        try {
            val store = storeRepo.getMyStore()
            if (store == null) {
                _state.update { it.copy(isLoading = false) }
            } else {
                val location = store.location?.let { LatLon(it.latitude, it.longitude) }
                _state.update {
                    it.copy(
                        isLoading = false,
                        name = store.name,
                        category = StoreCategory.fromValue(store.category),
                        pin = location,
                        mapCenter = location ?: it.mapCenter,
                        address = store.address,
                        postcode = store.postcode,
                        city = store.city
                    )
                }
            }
        } catch (e: StoreError) {
            _state.update { it.copy(isLoading = false, errorRes = e.toStringRes()) }
        }
    }

    // --- Invoervelden ---
    fun onNameChange(value: String) = _state.update { it.copy(name = value, errorRes = null) }
    fun onCategoryChange(value: StoreCategory) = _state.update { it.copy(category = value, errorRes = null) }
    fun onAddressChange(value: String) = _state.update { it.copy(address = value, errorRes = null) }
    fun onPostcodeChange(value: String) = _state.update { it.copy(postcode = value, errorRes = null) }
    fun onCityChange(value: String) = _state.update { it.copy(city = value, errorRes = null) }

    /** H1 + H2: pin gezet -> adres automatisch opzoeken. */
    fun onMapTap(point: LatLon) {
        _state.update { it.copy(pin = point, mapCenter = point, errorRes = null, infoRes = null) }

        geocodeJob?.cancel() // vorige zoekopdracht stoppen als je snel opnieuw tikt
        geocodeJob = viewModelScope.launch {
            _state.update { it.copy(isGeocoding = true) }
            try {
                val result = geocodingRepo.reverse(point.lat, point.lon)
                _state.update {
                    it.copy(
                        isGeocoding = false,
                        address = result.street,
                        postcode = result.postcode,
                        city = result.city
                    )
                }
            } catch (e: GeocodingError) {
                val message = when (e) {
                    GeocodingError.Network -> R.string.error_no_internet
                    GeocodingError.NotFound -> R.string.error_address_not_found
                }
                _state.update { it.copy(isGeocoding = false, errorRes = message) }
            }
        }
    }

    // --- "Gebruik mijn locatie" ---

    /** Knop "Gebruik mijn locatie": al toestemming? Meteen zoeken. Anders eerst uitleg tonen. */
    fun onUseMyLocationClick() {
        if (locationRepo.hasAnyPermission()) {
            findMyLocation(precise = locationRepo.hasFinePermission())
        } else {
            _state.update { it.copy(showLocationRationale = true, errorRes = null, infoRes = null) }
        }
    }

    /** Uitlegvenster gesloten (de gebruiker koos "Toestaan" of "Annuleren"). */
    fun onLocationRationaleDismiss() = _state.update { it.copy(showLocationRationale = false) }

    /** Antwoord van Android op de permissievraag: alle vier de gevallen afhandelen. */
    fun onLocationPermissionResult(result: LocationPermissionResult) {
        when (result) {
            LocationPermissionResult.PRECISE -> findMyLocation(precise = true)
            LocationPermissionResult.APPROXIMATE -> findMyLocation(precise = false)
            LocationPermissionResult.DENIED ->
                _state.update { it.copy(errorRes = R.string.error_location_denied) }
            LocationPermissionResult.PERMANENTLY_DENIED -> _state.update {
                it.copy(errorRes = R.string.error_location_denied_permanently, locationPermanentlyDenied = true)
            }
        }
    }

    /**
     * Precieze locatie: pin meteen zetten + adres opzoeken (zoals bij een tik op de kaart).
     * Benaderde locatie (kan honderden meters afwijken): enkel de kaart verplaatsen,
     * de handelaar zet de pin zelf.
     */
    private fun findMyLocation(precise: Boolean) {
        _state.update {
            it.copy(isLocating = true, locationPermanentlyDenied = false, errorRes = null, infoRes = null)
        }
        viewModelScope.launch {
            try {
                val point = locationRepo.getCurrentLocation()
                _state.update { it.copy(isLocating = false) }
                if (precise) {
                    onMapTap(point)
                } else {
                    _state.update { it.copy(mapCenter = point, infoRes = R.string.info_location_approximate) }
                }
            } catch (e: LocationError) {
                val message = when (e) {
                    LocationError.NoPermission -> R.string.error_location_denied
                    LocationError.Disabled -> R.string.error_location_disabled
                    LocationError.Unavailable -> R.string.error_location_unavailable
                }
                _state.update { it.copy(isLocating = false, errorRes = message) }
            }
        }
    }

    /** Controleren en opslaan in Firestore. */
    fun save() {
        val s = _state.value
        if (s.isSaving) return

        val error = when {
            s.name.isBlank() -> R.string.error_store_name_empty
            s.category == null -> R.string.error_store_category_missing
            s.pin == null -> R.string.error_store_pin_missing
            s.address.isBlank() -> R.string.error_store_address_empty
            s.city.isBlank() -> R.string.error_store_city_empty
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(errorRes = error) }
            return
        }
        if (!NetworkUtils.isOnline(getApplication<Application>())) {
            _state.update { it.copy(errorRes = R.string.error_no_internet) }
            return
        }

        _state.update { it.copy(isSaving = true, errorRes = null) }
        viewModelScope.launch {
            try {
                // De !! zijn veilig: de validatie hierboven heeft dit gecontroleerd
                storeRepo.saveMyStore(
                    name = s.name.trim(),
                    category = s.category!!.value,
                    location = s.pin!!,
                    address = s.address.trim(),
                    postcode = s.postcode.trim(),
                    city = s.city.trim()
                )
                _state.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: StoreError) {
                _state.update { it.copy(isSaving = false, errorRes = e.toStringRes()) }
            }
        }
    }

    private fun StoreError.toStringRes(): Int = when (this) {
        StoreError.NotLoggedIn -> R.string.error_not_logged_in
        StoreError.Network -> R.string.error_no_internet
        StoreError.Unknown -> R.string.error_unknown
    }
}
