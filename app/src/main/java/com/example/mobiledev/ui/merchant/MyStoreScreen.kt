package com.example.mobiledev.ui.merchant

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiledev.R
import com.example.mobiledev.data.model.StoreCategory
import com.example.mobiledev.ui.map.OsmMap

/** Scherm "Mijn winkel" (H1 + H2): naam, categorie, pin op de kaart, automatisch adres. */
@Composable
fun MyStoreScreen(
    onSaved: () -> Unit,
    viewModel: MyStoreViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.store_title), style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text(stringResource(R.string.field_store_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Categorie kiezen met "chips"
        Text(stringResource(R.string.store_category_label), style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StoreCategory.entries.forEach { category ->
                FilterChip(
                    selected = state.category == category,
                    onClick = { viewModel.onCategoryChange(category) },
                    label = { Text(stringResource(category.labelRes())) }
                )
            }
        }

        // Kaart: tik om de pin te zetten
        Text(stringResource(R.string.store_map_hint), style = MaterialTheme.typography.bodyMedium)
        OsmMap(
            center = state.mapCenter,
            pin = state.pin,
            onTap = viewModel::onMapTap,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        if (state.isGeocoding) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.store_geocoding), style = MaterialTheme.typography.bodySmall)
            }
        }

        // Adres: automatisch ingevuld, maar de handelaar kan het nog aanpassen (H2)
        OutlinedTextField(
            value = state.address,
            onValueChange = viewModel::onAddressChange,
            label = { Text(stringResource(R.string.field_store_address)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.postcode,
                onValueChange = viewModel::onPostcodeChange,
                label = { Text(stringResource(R.string.field_store_postcode)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.city,
                onValueChange = viewModel::onCityChange,
                label = { Text(stringResource(R.string.field_store_city)) },
                singleLine = true,
                modifier = Modifier.weight(2f)
            )
        }

        state.errorRes?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = viewModel::save,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.action_save_store))
            }
        }
    }
}

/** Welke tekst (uit strings.xml) hoort bij welke categorie. */
@StringRes
private fun StoreCategory.labelRes(): Int = when (this) {
    StoreCategory.BAKER -> R.string.category_baker
    StoreCategory.SUPERMARKET -> R.string.category_supermarket
    StoreCategory.RESTAURANT -> R.string.category_restaurant
    StoreCategory.BUTCHER -> R.string.category_butcher
    StoreCategory.GREENGROCER -> R.string.category_greengrocer
    StoreCategory.OTHER -> R.string.category_other
}
