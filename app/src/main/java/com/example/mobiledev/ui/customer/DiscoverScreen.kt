package com.example.mobiledev.ui.customer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiledev.R
import com.example.mobiledev.data.model.LatLon
import com.example.mobiledev.ui.location.openAppSettings
import com.example.mobiledev.ui.location.rememberLocationPermissionRequest
import com.example.mobiledev.ui.map.OsmMap

/** Tabblad "Ontdekken": "Pakketten in de buurt van {gemeente}" + lijst (K1-K3). */
@Composable
fun DiscoverScreen(
    onLogout: () -> Unit,
    viewModel: DiscoverViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val requestPermission = rememberLocationPermissionRequest(viewModel::onLocationPermissionResult)

    // Eerst uitleg, pas daarna de echte permissievraag
    if (state.showLocationRationale) {
        AlertDialog(
            onDismissRequest = viewModel::onRationaleCancel,
            title = { Text(stringResource(R.string.location_rationale_title)) },
            text = { Text(stringResource(R.string.location_rationale_customer)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onRationaleAllow()
                    requestPermission()
                }) { Text(stringResource(R.string.action_allow)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onRationaleCancel) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // K2: titel met gemeente
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val municipality = state.municipality
            Text(
                text = if (municipality != null) {
                    stringResource(R.string.discover_title_in, municipality)
                } else {
                    stringResource(R.string.discover_title)
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onLogout) { Text(stringResource(R.string.action_logout)) }
        }

        LocationSection(
            state = state,
            onUseMyLocation = viewModel::onUseMyLocationClick,
            onPickOnMap = viewModel::onChooseOnMapClick,
            onMapTap = viewModel::onMapTap,
            onDone = viewModel::onPickingDone,
            onOpenSettings = { openAppSettings(context) }
        )

        // K3: lijst
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val errorRes = state.errorRes
            when {
                state.isLoading -> CenteredBox { CircularProgressIndicator() }
                errorRes != null -> CenteredBox {
                    Text(stringResource(errorRes), color = MaterialTheme.colorScheme.error)
                }
                state.bags.isEmpty() -> CenteredBox {
                    Text(stringResource(R.string.discover_empty), textAlign = TextAlign.Center)
                }
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.bags, key = { it.id }) { bag -> DiscoverBagCard(bag) }
                }
            }
        }
    }
}

@Composable
private fun LocationSection(
    state: DiscoverUiState,
    onUseMyLocation: () -> Unit,
    onPickOnMap: () -> Unit,
    onMapTap: (LatLon) -> Unit,
    onDone: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.isLocating) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.location_finding), style = MaterialTheme.typography.bodySmall)
            }
        }

        // Nog geen locatie en niet aan het kiezen: uitleg + twee keuzes
        if (state.location == null && !state.isPickingOnMap && !state.isLocating) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.discover_location_intro))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onUseMyLocation) {
                            Text(stringResource(R.string.action_use_my_location))
                        }
                        OutlinedButton(onClick = onPickOnMap) {
                            Text(stringResource(R.string.action_pick_on_map))
                        }
                    }
                }
            }
        }

        // Melding over de locatie (geweigerd, uit, gemeente niet gevonden, ...)
        state.locationMessageRes?.let {
            Text(
                stringResource(it),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (state.locationPermanentlyDenied) {
            TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.action_open_settings)) }
        }

        // Fallback: pin op de kaart zetten
        if (state.isPickingOnMap) {
            Text(stringResource(R.string.discover_map_hint), style = MaterialTheme.typography.bodySmall)
            OsmMap(
                center = state.location ?: DiscoverUiState.DEFAULT_CENTER,
                pin = state.location,
                zoom = 14.0,
                onTap = onMapTap,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Button(onClick = onDone, enabled = state.location != null) {
                Text(stringResource(R.string.action_done))
            }
        } else if (state.location != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onPickOnMap) { Text(stringResource(R.string.action_change_location)) }
                TextButton(onClick = onUseMyLocation) { Text(stringResource(R.string.action_use_my_location)) }
            }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

/** K3: foto (klein), winkelnaam, titel, prijs (met doorstreepte waarde), ophaalvenster, aantal, afstand. */
@Composable
private fun DiscoverBagCard(bag: DiscoverBagItem) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                bag.photo?.let {
                    Image(
                        bitmap = it,
                        contentDescription = stringResource(R.string.bag_photo_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(bag.storeName, style = MaterialTheme.typography.labelMedium)
                Text(bag.title, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.price_format, bag.price),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.price_format, bag.originalValue),
                        textDecoration = TextDecoration.LineThrough,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    stringResource(R.string.bag_pickup_window, bag.pickupStart, bag.pickupEnd),
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.bag_left, bag.quantityLeft),
                        style = MaterialTheme.typography.bodySmall
                    )
                    bag.distanceKm?.let {
                        Text(
                            stringResource(R.string.distance_km, it),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}