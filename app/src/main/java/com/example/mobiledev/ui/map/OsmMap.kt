package com.example.mobiledev.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.mobiledev.data.model.LatLon
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/**
 * Herbruikbare OSMDroid-kaart voor Compose.
 * - center: waar de kaart naartoe beweegt
 * - pin: waar de marker staat (null = geen marker)
 * - onTap: wordt opgeroepen als je op de kaart tikt (null = tikken doet niets)
 */
@Composable
fun OsmMap(
    center: LatLon,
    pin: LatLon?,
    modifier: Modifier = Modifier,
    zoom: Double = 16.0,
    onTap: ((LatLon) -> Unit)? = null
) {
    val context = LocalContext.current
    val currentOnTap by rememberUpdatedState(onTap)

    // De MapView maken we maar één keer (remember)
    val mapView = remember {
        MapView(context).apply {
            setTileSource(OsmConfig.TILE_SOURCE) // onze eigen kaartbron (zie OsmConfig)
            setMultiTouchControls(true) // inzoomen met twee vingers
            controller.setZoom(zoom)
            controller.setCenter(GeoPoint(center.lat, center.lon))

            // Tikken op de kaart opvangen (H1: pin zetten)
            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    val handler = currentOnTap ?: return false
                    handler(LatLon(p.latitude, p.longitude))
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean = false
            }))

            // Slepen op de kaart = kaart verschuiven, niet het hele scherm scrollen
            setOnTouchListener { view, _ ->
                view.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }

    val marker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setInfoWindow(null) // geen tekstballon bij tikken op de marker
        }
    }

    // Levenscyclus van de MapView volgen (verplicht voor OSMDroid)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() } // opruimen als het scherm verdwijnt
    }

    // Als het centrum verandert: de kaart er vloeiend naartoe bewegen
    LaunchedEffect(center) {
        mapView.controller.animateTo(GeoPoint(center.lat, center.lon))
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            if (pin != null) {
                marker.position = GeoPoint(pin.lat, pin.lon)
                if (!view.overlays.contains(marker)) view.overlays.add(marker)
            } else {
                view.overlays.remove(marker)
            }
            view.invalidate() // kaart opnieuw tekenen
        }
    )
}