package kr.co.root.legolas.location.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kr.co.root.legolas.location.R
import kr.co.root.legolas.location.data.LocationPlace
import kr.co.root.legolas.location.data.LocationPlaceDraft
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationPlaceEditor(
    place: LocationPlace?,
    suggestedLatitude: Double?,
    suggestedLongitude: Double?,
    isExternalBasemapEnabled: Boolean,
    isSaving: Boolean,
    hasError: Boolean,
    onEnableExternalBasemap: () -> Unit,
    onSave: (LocationPlaceDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(place?.id) { mutableStateOf(place?.name.orEmpty()) }
    var latitude by rememberSaveable(place?.id, suggestedLatitude) {
        mutableStateOf(place?.latitude?.coordinateText() ?: suggestedLatitude?.coordinateText().orEmpty())
    }
    var longitude by rememberSaveable(place?.id, suggestedLongitude) {
        mutableStateOf(place?.longitude?.coordinateText() ?: suggestedLongitude?.coordinateText().orEmpty())
    }
    var radius by rememberSaveable(place?.id) {
        mutableStateOf((place?.radiusMeters ?: DefaultPlaceRadiusMeters).numberText())
    }
    val draft = locationPlaceDraftOrNull(name, latitude, longitude, radius)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                if (place == null) {
                                    R.string.location_place_add_title
                                } else {
                                    R.string.location_place_edit_title
                                },
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = !isSaving) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.location_back),
                            )
                        }
                    },
                )
            },
            bottomBar = {
                Surface(shadowElevation = 6.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = { draft?.let(onSave) },
                            enabled = draft != null && !isSaving,
                        ) {
                            Text(stringResource(R.string.location_place_save))
                        }
                    }
                }
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.location_place_editor_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.location_place_name)) },
                        singleLine = true,
                        isError = name.length > 80,
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = latitude,
                            onValueChange = { latitude = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.location_place_latitude)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = longitude,
                            onValueChange = { longitude = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.location_place_longitude)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = radius,
                            onValueChange = { radius = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.location_place_radius)) },
                            suffix = { Text(stringResource(R.string.location_meters_short)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                        )
                        Slider(
                            value = (radius.toFloatOrNull() ?: DefaultPlaceRadiusMeters.toFloat())
                                .coerceIn(MinimumRadiusSlider, MaximumRadiusSlider),
                            onValueChange = { radius = it.roundToInt().toString() },
                            valueRange = MinimumRadiusSlider..MaximumRadiusSlider,
                            steps = RadiusSliderSteps,
                        )
                    }
                }
                item {
                    val mapLatitude = latitude.toDoubleOrNull()?.takeIf { it in -90.0..90.0 }
                    val mapLongitude = longitude.toDoubleOrNull()?.takeIf { it in -180.0..180.0 }
                    if (isExternalBasemapEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.location_place_map_instruction),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            LocationPlaceMap(
                                latitude = mapLatitude,
                                longitude = mapLongitude,
                                radiusMeters = radius.toDoubleOrNull() ?: DefaultPlaceRadiusMeters,
                                onPositionSelected = { selectedLatitude, selectedLongitude ->
                                    latitude = selectedLatitude.coordinateText()
                                    longitude = selectedLongitude.coordinateText()
                                },
                            )
                        }
                    } else {
                        PlacesMessageCard(
                            title = stringResource(R.string.location_place_map_private_title),
                            description = stringResource(R.string.location_place_map_private_description),
                            action = {
                                OutlinedButton(onClick = onEnableExternalBasemap) {
                                    Text(stringResource(R.string.location_use_external_basemap))
                                }
                            },
                        )
                    }
                }
                if (draft == null) {
                    item {
                        Text(
                            text = stringResource(R.string.location_place_validation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (hasError) {
                    item {
                        Text(
                            text = stringResource(R.string.location_places_update_error_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

internal fun locationPlaceDraftOrNull(
    name: String,
    latitude: String,
    longitude: String,
    radiusMeters: String,
): LocationPlaceDraft? {
    val normalizedName = name.trim()
    val parsedLatitude = latitude.toDoubleOrNull()
    val parsedLongitude = longitude.toDoubleOrNull()
    val parsedRadius = radiusMeters.toDoubleOrNull()
    return LocationPlaceDraft(
        name = normalizedName,
        latitude = parsedLatitude ?: return null,
        longitude = parsedLongitude ?: return null,
        radiusMeters = parsedRadius ?: return null,
    ).takeIf {
        normalizedName.isNotEmpty() &&
            normalizedName.length <= 80 &&
            parsedLatitude in -90.0..90.0 &&
            parsedLongitude in -180.0..180.0 &&
            parsedRadius > 0.0 &&
            parsedRadius <= 10_000.0
    }
}

private fun Double.coordinateText(): String = String.format(Locale.US, "%.6f", this)

private fun Double.numberText(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()

private const val DefaultPlaceRadiusMeters = 50.0
private const val MinimumRadiusSlider = 10f
private const val MaximumRadiusSlider = 500f
private const val RadiusSliderSteps = 48
