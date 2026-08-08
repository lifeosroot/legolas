package kr.co.root.legolas.location.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kr.co.root.legolas.location.R
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position

@Composable
internal fun LocationPlaceMap(
    latitude: Double?,
    longitude: Double?,
    radiusMeters: Double,
    onPositionSelected: (latitude: Double, longitude: Double) -> Unit,
) {
    val hasPosition = latitude != null && longitude != null
    val cameraState = rememberCameraState(
        CameraPosition(
            target = Position(longitude ?: 0.0, latitude ?: 0.0),
            zoom = if (hasPosition) 15.0 else 1.0,
        ),
    )
    var mapLoadFailed by remember { mutableStateOf(false) }
    val description = stringResource(R.string.location_place_map_accessibility)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        MaplibreMap(
            baseStyle = BaseStyle.Uri(OpenFreeMapStyleUrl),
            cameraState = cameraState,
            onMapClick = { position, _ ->
                onPositionSelected(position.latitude, position.longitude)
                ClickResult.Consume
            },
            onMapLoadFailed = { mapLoadFailed = true },
        ) {
            if (hasPosition) {
                val point = remember(latitude, longitude) {
                    "{\"type\":\"Point\",\"coordinates\":[$longitude,$latitude]}"
                }
                val source = rememberGeoJsonSource(GeoJsonData.JsonString(point))
                val metersPerDp = cameraState.metersPerDpAtTarget
                val radiusDp = if (metersPerDp > 0.0) {
                    (radiusMeters / metersPerDp).coerceIn(3.0, 600.0).toFloat().dp
                } else {
                    8.dp
                }
                val areaColor = MaterialTheme.colorScheme.primary
                val markerColor = MaterialTheme.colorScheme.tertiary
                val strokeColor = MaterialTheme.colorScheme.surface

                CircleLayer(
                    id = "legolas-place-radius",
                    source = source,
                    color = const(areaColor),
                    opacity = const(0.2f),
                    radius = const(radiusDp),
                )
                CircleLayer(
                    id = "legolas-place-center",
                    source = source,
                    color = const(markerColor),
                    radius = const(7.dp),
                    strokeColor = const(strokeColor),
                    strokeWidth = const(2.dp),
                )
            }
        }

        if (mapLoadFailed) {
            Text(
                text = stringResource(R.string.location_external_basemap_unavailable),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
