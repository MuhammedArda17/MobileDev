package com.example.mobiledev.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/** Retrofit-interface voor de /reverse endpoint van Nominatim. */
interface NominatimApi {

    @GET("reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("zoom") zoom: Int = 18,            // 18 = gebouw (straat + huisnummer)
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("accept-language") language: String = "nl" // "Antwerpen" i.p.v. "Anvers"
    ): NominatimResponse
}

/** Het antwoord van Nominatim (enkel de velden die we nodig hebben). */
data class NominatimResponse(
    @SerializedName("display_name") val displayName: String?,
    val address: NominatimAddress?
)

data class NominatimAddress(
    val road: String?,
    @SerializedName("house_number") val houseNumber: String?,
    val postcode: String?,
    // afhankelijk van de plaats krijg je city, town of village
    val city: String?,
    val town: String?,
    val village: String?,
    val municipality: String?
)
