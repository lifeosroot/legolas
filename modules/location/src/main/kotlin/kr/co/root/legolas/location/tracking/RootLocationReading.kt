package kr.co.root.legolas.location.tracking

import android.location.Location
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class RootLocationReading(
    val timeMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val speedMps: Float?,
)

internal fun Location.toRootLocationReading(): RootLocationReading? {
    if (!latitude.isFinite() ||
        !longitude.isFinite() ||
        latitude !in -90.0..90.0 ||
        longitude !in -180.0..180.0
    ) {
        return null
    }

    return RootLocationReading(
        timeMillis = time,
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        speedMps = if (hasSpeed()) speed else null,
    )
}

internal fun RootLocationReading.distanceMetersTo(other: RootLocationReading): Float {
    val lat1 = latitude.toRadians()
    val lat2 = other.latitude.toRadians()
    val deltaLat = (other.latitude - latitude).toRadians()
    val deltaLng = (other.longitude - longitude).toRadians()

    val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
        cos(lat1) * cos(lat2) * sin(deltaLng / 2) * sin(deltaLng / 2)
    val c = 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    return (EarthRadiusMeters * c).toFloat()
}

private fun Double.toRadians(): Double = this * PI / 180.0

private const val EarthRadiusMeters = 6_371_000.0

