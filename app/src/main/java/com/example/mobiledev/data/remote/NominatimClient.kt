package com.example.mobiledev.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Maakt één Retrofit-instantie voor Nominatim. */
object NominatimClient {

    private const val BASE_URL = "https://nominatim.openstreetmap.org/"

    // Verplicht (gebruiksvoorwaarden): een User-Agent die ONZE app identificeert.
    // Een nep-browsernaam is verboden.
    private const val USER_AGENT = "MobileDevGroep18/1.0 (AP Hogeschool student project)"

    val api: NominatimApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)
    }
}
