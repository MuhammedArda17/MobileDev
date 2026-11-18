package com.example.mobiledev.ui.merchant

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiledev.R
import java.time.LocalTime

/**
 * Scherm "Pakket aanmaken" (H3 + H4).
 * Bevat GEEN Firebase-code en GEEN logica: alles gaat naar de ViewModel.
 */
@Composable
fun AddBagScreen(
    onSaved: () -> Unit,
    viewModel: AddBagViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Als het pakket opgeslagen is: terug naar "Mijn pakketten"
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    // Foto kiezen uit de galerij (Photo Picker: geen permissie nodig)
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri)
    }

    // Welke tijdkiezer staat open? rememberSaveable -> blijft open na rotatie
    var showStartPicker by rememberSaveable { mutableStateOf(false) }
    var showEndPicker by rememberSaveable { mutableStateOf(false) }

    val notSet = stringResource(R.string.time_not_set)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.add_bag_title), style = MaterialTheme.typography.headlineMedium)

        PhotoBox(
            preview = state.photoPreview,
            isProcessing = state.isProcessingPhoto,
            onClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )

        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::onTitleChange,
            label = { Text(stringResource(R.string.field_bag_title)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text(stringResource(R.string.field_bag_description)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.originalValue,
                onValueChange = viewModel::onOriginalValueChange,
                label = { Text(stringResource(R.string.field_bag_original_value)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.price,
                onValueChange = viewModel::onPriceChange,
                label = { Text(stringResource(R.string.field_bag_price)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = state.quantity,
            onValueChange = viewModel::onQuantityChange,
            label = { Text(stringResource(R.string.field_bag_quantity)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Ophaalvenster (begin- en einduur van vandaag)
        OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.bag_pickup_start, state.pickupStartMinutes?.toTimeText() ?: notSet))
        }
        OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.bag_pickup_end, state.pickupEndMinutes?.toTimeText() ?: notSet))
        }

        state.errorRes?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = viewModel::save,
            enabled = !state.isSaving && !state.isProcessingPhoto,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.action_save_bag))
            }
        }
    }

    if (showStartPicker) {
        TimePickerDialog(
            initialMinutes = state.pickupStartMinutes,
            onConfirm = { viewModel.onPickupStartChange(it); showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        TimePickerDialog(
            initialMinutes = state.pickupEndMinutes,
            onConfirm = { viewModel.onPickupEndChange(it); showEndPicker = false },
            onDismiss = { showEndPicker = false }
        )
    }
}

/** Vak bovenaan: toont de foto, een laadcirkel, of "Tik om een foto te kiezen". */
@Composable
private fun PhotoBox(preview: ImageBitmap?, isProcessing: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = !isProcessing, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            isProcessing -> CircularProgressIndicator()
            preview != null -> Image(
                bitmap = preview,
                contentDescription = stringResource(R.string.bag_photo_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            else -> Text(
                stringResource(R.string.bag_photo_pick),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Dialoog met een klok (24-uursformaat). Geeft het gekozen uur terug in minuten. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialMinutes: Int?,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialMinutes?.div(60) ?: LocalTime.now().hour,
        initialMinute = initialMinutes?.rem(60) ?: 0,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour * 60 + pickerState.minute) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        text = { TimePicker(state = pickerState) }
    )
}
