package com.example.mobiledev.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtils {
    /**
     * Is er internet? Firestore wacht bij schrijven zonder internet gewoon tot er
     * weer verbinding is (de laadindicator blijft dan draaien). Daarom checken we
     * dit zelf vóór het opslaan, zodat we een duidelijke melding kunnen tonen (G1).
     */
    fun isOnline(context: Context): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
