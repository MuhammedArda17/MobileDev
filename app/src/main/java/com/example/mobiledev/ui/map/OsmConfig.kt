package com.example.mobiledev.ui.map

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy
import org.osmdroid.tileprovider.tilesource.XYTileSource
import java.io.File

/**
 * Instellingen voor OSMDroid. Eén keer oproepen bij het opstarten (in MainActivity),
 * VOOR er een kaart getoond wordt.
 */
object OsmConfig {

    // Verplicht: een UNIEKE naam voor onze app. "com.example..." is te algemeen
    // en wordt geblokkeerd door de kaartservers van OpenStreetMap.
    private const val USER_AGENT = "MobileDevGroep18-APHogeschool/1.0"

    /**
     * Dezelfde kaart als TileSourceFactory.MAPNIK, maar ZONDER de vlag
     * FLAG_USER_AGENT_NORMALIZED. Met die vlag stuurt OSMDroid onze pakketnaam
     * ("com.example.mobiledev/1") i.p.v. onze eigen User-Agent -> geblokkeerd.
     */
    val TILE_SOURCE = XYTileSource(
        "OSM-Groep18",            // eigen naam = eigen cache
        0, 19, 256, ".png",
        arrayOf("https://tile.openstreetmap.org/"),
        "© OpenStreetMap contributors",
        TileSourcePolicy(
            2, // max. 2 downloads tegelijk
            TileSourcePolicy.FLAG_NO_BULK or
                    TileSourcePolicy.FLAG_NO_PREVENTIVE or
                    TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL
        )
    )

    fun init(context: Context) {
        val config = Configuration.getInstance()
        config.load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        config.userAgentValue = USER_AGENT
        // Kaartjes bewaren in de cache-map van de app (geen extra permissie nodig)
        config.osmdroidBasePath = File(context.cacheDir, "osmdroid-v2")
        config.osmdroidTileCache = File(context.cacheDir, "osmdroid-v2/tiles")
    }
}