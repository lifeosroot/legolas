package kr.co.root.legolas.location.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kr.co.root.legolas.location.R
import kr.co.root.legolas.location.data.LocationSample
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.BoundingBox

@Composable
internal fun LocationRouteMapCard(
    samples: List<LocationSample>,
    isExternalBasemapEnabled: Boolean,
    onEnableExternalBasemap: () -> Unit,
    onDisableExternalBasemap: () -> Unit,
) {
    var mapLoadFailed by remember(isExternalBasemapEnabled, samples) { mutableStateOf(false) }
    var isMapDialogOpen by rememberSaveable { mutableStateOf(false) }
    val openMapDescription = stringResource(R.string.location_open_fullscreen_map)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.location_route_map),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { isMapDialogOpen = true }) {
                    Text(stringResource(R.string.location_view_larger))
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(MaterialTheme.shapes.medium),
            ) {
                RouteMap(
                    samples = samples,
                    showExternalBasemap = isExternalBasemapEnabled && !mapLoadFailed,
                    onLoadFailed = { mapLoadFailed = true },
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .semantics { contentDescription = openMapDescription }
                        .clickable(
                            role = Role.Button,
                            onClick = { isMapDialogOpen = true },
                        ),
                )
            }

            Text(
                text = stringResource(
                    when {
                        mapLoadFailed -> R.string.location_external_basemap_unavailable
                        isExternalBasemapEnabled -> R.string.location_external_basemap_active
                        else -> R.string.location_local_route_preview_description
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    !isExternalBasemapEnabled -> Button(onClick = onEnableExternalBasemap) {
                        Text(stringResource(R.string.location_use_external_basemap))
                    }

                    mapLoadFailed -> {
                        Button(onClick = { mapLoadFailed = false }) {
                            Text(stringResource(R.string.location_retry))
                        }
                        OutlinedButton(onClick = onDisableExternalBasemap) {
                            Text(stringResource(R.string.location_disable_external_basemap))
                        }
                    }

                    else -> OutlinedButton(onClick = onDisableExternalBasemap) {
                        Text(stringResource(R.string.location_disable_external_basemap))
                    }
                }
            }
        }
    }

    if (isMapDialogOpen) {
        RouteMapDialog(
            samples = samples,
            showExternalBasemap = isExternalBasemapEnabled && !mapLoadFailed,
            onLoadFailed = { mapLoadFailed = true },
            onDismiss = { isMapDialogOpen = false },
        )
    }
}

@Composable
private fun RouteMap(
    samples: List<LocationSample>,
    showExternalBasemap: Boolean,
    onLoadFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (showExternalBasemap) {
            ExternalRouteMap(
                samples = samples,
                onLoadFailed = onLoadFailed,
            )
        } else {
            LocalRoutePreview(samples)
        }
    }
}

@Composable
private fun RouteMapDialog(
    samples: List<LocationSample>,
    showExternalBasemap: Boolean,
    onLoadFailed: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.location_route_map),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.location_close))
                }
            }
            RouteMap(
                samples = samples,
                showExternalBasemap = showExternalBasemap,
                onLoadFailed = onLoadFailed,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large),
            )
        }
    }
}

@Composable
private fun ExternalRouteMap(
    samples: List<LocationSample>,
    onLoadFailed: () -> Unit,
) {
    val bounds = remember(samples) {
        samples.firstOrNull()?.let { samples.routeBounds().toMapLibreBounds() }
    }
    val cameraState = rememberCameraState()
    var isMapLoaded by remember { mutableStateOf(false) }
    val routeColor = MaterialTheme.colorScheme.primary
    val startColor = MaterialTheme.colorScheme.tertiary
    val endColor = MaterialTheme.colorScheme.error
    val markerStrokeColor = MaterialTheme.colorScheme.surface

    LaunchedEffect(isMapLoaded, bounds) {
        if (isMapLoaded && bounds != null) {
            cameraState.animateTo(
                boundingBox = bounds,
                padding = PaddingValues(36.dp),
            )
        }
    }

    MaplibreMap(
        modifier = Modifier.fillMaxSize(),
        baseStyle = BaseStyle.Uri(OpenFreeMapStyleUrl),
        cameraState = cameraState,
        onMapLoadFailed = { onLoadFailed() },
        onMapLoadFinished = { isMapLoaded = true },
    ) {
        if (samples.isNotEmpty()) {
            val routeGeoJson = remember(samples) { samples.routeLineGeoJson() }
            val startGeoJson = remember(samples) { samples.first().routePointGeoJson() }
            val routeSource = rememberGeoJsonSource(GeoJsonData.JsonString(routeGeoJson))
            val startSource = rememberGeoJsonSource(GeoJsonData.JsonString(startGeoJson))

            LineLayer(
                id = "legolas-route-line",
                source = routeSource,
                color = const(routeColor),
                width = const(5.dp),
            )
            CircleLayer(
                id = "legolas-route-start",
                source = startSource,
                color = const(startColor),
                radius = const(7.dp),
                strokeColor = const(markerStrokeColor),
                strokeWidth = const(2.dp),
            )
            if (samples.size > 1) {
                val endGeoJson = remember(samples) { samples.last().routePointGeoJson() }
                val endSource = rememberGeoJsonSource(GeoJsonData.JsonString(endGeoJson))
                CircleLayer(
                    id = "legolas-route-end",
                    source = endSource,
                    color = const(endColor),
                    radius = const(7.dp),
                    strokeColor = const(markerStrokeColor),
                    strokeWidth = const(2.dp),
                )
            }
        }
    }
}

@Composable
private fun LocalRoutePreview(samples: List<LocationSample>) {
    val bounds = remember(samples) { samples.firstOrNull()?.let { samples.routeBounds() } }
    val routeColor = MaterialTheme.colorScheme.primary
    val startColor = MaterialTheme.colorScheme.tertiary
    val endColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val backgroundColor = MaterialTheme.colorScheme.surface
    val description = stringResource(
        if (samples.isEmpty()) {
            R.string.location_local_route_preview_empty_accessibility
        } else {
            R.string.location_local_route_preview_accessibility
        },
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp)
            .semantics { contentDescription = description },
    ) {
        for (step in 1..3) {
            val fraction = step / 4f
            drawLine(
                color = gridColor,
                start = Offset(size.width * fraction, 0f),
                end = Offset(size.width * fraction, size.height),
            )
            drawLine(
                color = gridColor,
                start = Offset(0f, size.height * fraction),
                end = Offset(size.width, size.height * fraction),
            )
        }

        if (bounds != null) {
            fun LocationSample.offset(): Offset {
                val x = ((longitude - bounds.west) / (bounds.east - bounds.west)).toFloat() * size.width
                val y = size.height -
                    ((latitude - bounds.south) / (bounds.north - bounds.south)).toFloat() * size.height
                return Offset(x, y)
            }

            samples.zipWithNext().forEach { (from, to) ->
                drawLine(
                    color = routeColor,
                    start = from.offset(),
                    end = to.offset(),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            drawCircle(startColor, radius = 7.dp.toPx(), center = samples.first().offset())
            if (samples.size > 1) {
                drawCircle(endColor, radius = 7.dp.toPx(), center = samples.last().offset())
            }
        }
    }
}

@Composable
internal fun ExternalBasemapDisclosureDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.location_external_basemap_disclosure_title)) },
        text = { Text(stringResource(R.string.location_external_basemap_disclosure)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.location_use_external_basemap))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.location_keep_local_preview))
            }
        },
    )
}

internal data class RouteBounds(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
)

internal fun List<LocationSample>.routeBounds(): RouteBounds {
    require(isNotEmpty())
    val west = minOf { it.longitude }
    val east = maxOf { it.longitude }
    val south = minOf { it.latitude }
    val north = maxOf { it.latitude }
    val longitudePadding = if (west == east) MinimumBoundsPaddingDegrees else 0.0
    val latitudePadding = if (south == north) MinimumBoundsPaddingDegrees else 0.0
    // ponytail: simple min/max bounds; handle antimeridian wrapping if global routes need it.
    return RouteBounds(
        west = west - longitudePadding,
        south = south - latitudePadding,
        east = east + longitudePadding,
        north = north + latitudePadding,
    )
}

internal fun List<LocationSample>.routeLineGeoJson(): String {
    require(isNotEmpty())
    val coordinates = if (size == 1) listOf(first(), first()) else this
    return coordinates.joinToString(
        separator = ",",
        prefix = "{\"type\":\"LineString\",\"coordinates\":[",
        postfix = "]}",
    ) { sample -> "[${sample.longitude},${sample.latitude}]" }
}

private fun LocationSample.routePointGeoJson(): String =
    "{\"type\":\"Point\",\"coordinates\":[$longitude,$latitude]}"

private fun RouteBounds.toMapLibreBounds(): BoundingBox = BoundingBox(
    west = west,
    south = south,
    east = east,
    north = north,
)

internal const val OpenFreeMapStyleUrl = "https://tiles.openfreemap.org/styles/liberty"
private const val MinimumBoundsPaddingDegrees = 0.002
