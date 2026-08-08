package kr.co.root.legolas.location.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.co.root.legolas.location.R
import kr.co.root.legolas.location.data.LocationPlace
import kr.co.root.legolas.location.permission.hasLocationServerAccess

@Composable
internal fun LocationPlacesTab(
    serverUrl: String,
    modifier: Modifier = Modifier,
    viewModel: LocationPlacesViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasServerAccess by remember(serverUrl) {
        mutableStateOf(context.hasLocationServerAccess(serverUrl))
    }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingPlace by remember { mutableStateOf<LocationPlace?>(null) }
    var deletingPlace by remember { mutableStateOf<LocationPlace?>(null) }
    var showBasemapDisclosure by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(context, serverUrl) {
        val currentAccess = context.hasLocationServerAccess(serverUrl)
        if (!hasServerAccess && currentAccess) viewModel.refresh()
        hasServerAccess = currentAccess
        onPauseOrDispose { }
    }
    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasServerAccess = granted || context.hasLocationServerAccess(serverUrl)
        if (hasServerAccess) viewModel.refresh()
    }

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            editorOpen = false
            viewModel.consumeSaveCompleted()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.location_places_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(R.string.location_places_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = {
                        viewModel.clearMutationError()
                        editingPlace = null
                        editorOpen = true
                    },
                    enabled = hasServerAccess && !state.isLoading,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.location_place_add))
                }
            }
        }

        if (state.isLoading) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        when {
            !hasServerAccess -> item {
                PlacesMessageCard(
                    title = stringResource(R.string.location_route_local_network_title),
                    description = stringResource(R.string.location_places_local_network_description),
                    action = {
                        Button(
                            onClick = {
                                localNetworkPermissionLauncher.launch(
                                    Manifest.permission.ACCESS_LOCAL_NETWORK,
                                )
                            },
                        ) {
                            Text(stringResource(R.string.allow_local_network))
                        }
                    },
                )
            }

            state.hasLoadError -> item {
                PlacesMessageCard(
                    title = stringResource(R.string.location_places_error_title),
                    description = stringResource(R.string.location_places_error_description),
                    action = {
                        Button(onClick = viewModel::refresh) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text(stringResource(R.string.location_retry))
                        }
                    },
                )
            }

            state.isLoading && state.places.isEmpty() -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.places.isEmpty() -> item {
                PlacesMessageCard(
                    title = stringResource(R.string.location_places_empty_title),
                    description = stringResource(R.string.location_places_empty_description),
                )
            }

            else -> {
                if (state.hasMutationError && !editorOpen) {
                    item {
                        PlacesMessageCard(
                            title = stringResource(R.string.location_places_update_error_title),
                            description = stringResource(R.string.location_places_update_error_description),
                        )
                    }
                }
                items(state.places, key = LocationPlace::id) { place ->
                    LocationPlaceCard(
                        place = place,
                        enabled = !state.isSaving,
                        onEdit = {
                            viewModel.clearMutationError()
                            editingPlace = place
                            editorOpen = true
                        },
                        onDelete = { deletingPlace = place },
                    )
                }
            }
        }
    }

    if (editorOpen) {
        LocationPlaceEditor(
            place = editingPlace,
            suggestedLatitude = state.suggestedLatitude,
            suggestedLongitude = state.suggestedLongitude,
            isExternalBasemapEnabled = state.isExternalBasemapEnabled,
            isSaving = state.isSaving,
            hasError = state.hasMutationError,
            onEnableExternalBasemap = { showBasemapDisclosure = true },
            onSave = { draft -> viewModel.save(editingPlace?.id, draft) },
            onDismiss = { if (!state.isSaving) editorOpen = false },
        )
    }

    deletingPlace?.let { place ->
        AlertDialog(
            onDismissRequest = { if (!state.isSaving) deletingPlace = null },
            title = { Text(stringResource(R.string.location_place_delete_title)) },
            text = { Text(stringResource(R.string.location_place_delete_message, place.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingPlace = null
                        viewModel.delete(place.id)
                    },
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.location_place_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deletingPlace = null },
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.location_cancel))
                }
            },
        )
    }

    if (showBasemapDisclosure) {
        ExternalBasemapDisclosureDialog(
            onConfirm = {
                showBasemapDisclosure = false
                viewModel.setExternalBasemapEnabled(true)
            },
            onDismiss = { showBasemapDisclosure = false },
        )
    }
}

@Composable
private fun LocationPlaceCard(
    place: LocationPlace,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        ListItem(
            headlineContent = { Text(place.name) },
            supportingContent = {
                Text(
                    stringResource(
                        R.string.location_place_summary,
                        place.latitude,
                        place.longitude,
                        place.radiusMeters,
                    ),
                )
            },
            trailingContent = {
                Row {
                    IconButton(onClick = onEdit, enabled = enabled) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.location_place_edit),
                        )
                    }
                    IconButton(onClick = onDelete, enabled = enabled) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.location_place_delete),
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

@Composable
internal fun PlacesMessageCard(
    title: String,
    description: String,
    action: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            action?.invoke()
        }
    }
}
