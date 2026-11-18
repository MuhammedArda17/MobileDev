package com.example.mobiledev.ui.merchant

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiledev.R

/** Startscherm van de handelaar: "Mijn pakketten" + knop "Nieuw pakket" (H5). */
@Composable
fun MyBagsScreen(
    onAddBag: () -> Unit,
    onOpenStore: () -> Unit,
    onLogout: () -> Unit,
    viewModel: MyBagsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorRes = state.errorRes

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddBag) {
                Text(stringResource(R.string.action_new_bag))
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.my_bags_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onOpenStore) { Text(stringResource(R.string.action_my_store)) }
                TextButton(onClick = onLogout) { Text(stringResource(R.string.action_logout)) }
            }

            when {
                state.isLoading -> CenteredBox { CircularProgressIndicator() }
                errorRes != null -> CenteredBox {
                    Text(stringResource(errorRes), color = MaterialTheme.colorScheme.error)
                }
                state.bags.isEmpty() -> CenteredBox {
                    Text(stringResource(R.string.my_bags_empty), textAlign = TextAlign.Center)
                }
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp) // ruimte voor de knop
                ) {
                    items(state.bags, key = { it.id }) { bag -> BagRow(bag) }
                }
            }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun BagRow(bag: BagListItem) {
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
                Text(
                    stringResource(R.string.bag_stock, bag.quantityLeft, bag.reservedCount),
                    style = MaterialTheme.typography.bodySmall
                )
                if (!bag.active) {
                    Text(
                        stringResource(R.string.bag_offline),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
