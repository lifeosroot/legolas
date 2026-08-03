package kr.co.root.legolas.location.ui

import java.time.Instant
import kr.co.root.legolas.location.data.LocationSample
import kr.co.root.legolas.location.data.LocationSampleQuality
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationRouteMapTest {
    @Test
    fun `single sample bounds have usable area`() {
        val bounds = listOf(sample(latitude = 37.5, longitude = 127.0)).routeBounds()

        assertEquals(126.998, bounds.west, Tolerance)
        assertEquals(37.498, bounds.south, Tolerance)
        assertEquals(127.002, bounds.east, Tolerance)
        assertEquals(37.502, bounds.north, Tolerance)
    }

    @Test
    fun `route bounds contain every sample`() {
        val bounds = listOf(
            sample(latitude = 37.51, longitude = 127.03),
            sample(latitude = 37.47, longitude = 126.98),
            sample(latitude = 37.49, longitude = 127.01),
        ).routeBounds()

        assertEquals(126.98, bounds.west, Tolerance)
        assertEquals(37.47, bounds.south, Tolerance)
        assertEquals(127.03, bounds.east, Tolerance)
        assertEquals(37.51, bounds.north, Tolerance)
    }

    @Test
    fun `route geojson uses longitude latitude order`() {
        val geoJson = listOf(
            sample(latitude = 37.5, longitude = 127.0),
            sample(latitude = 37.6, longitude = 127.1),
        ).routeLineGeoJson()

        assertEquals(
            "{\"type\":\"LineString\",\"coordinates\":[[127.0,37.5],[127.1,37.6]]}",
            geoJson,
        )
    }

    @Test
    fun `single sample creates valid two-position line`() {
        val geoJson = listOf(sample(latitude = 37.5, longitude = 127.0)).routeLineGeoJson()

        assertEquals(
            "{\"type\":\"LineString\",\"coordinates\":[[127.0,37.5],[127.0,37.5]]}",
            geoJson,
        )
    }

    private fun sample(latitude: Double, longitude: Double) = LocationSample(
        id = 1L,
        collectedAt = Instant.EPOCH,
        latitude = latitude,
        longitude = longitude,
        horizontalAccuracyM = 5.0,
        source = "GPS",
        quality = LocationSampleQuality.GOOD,
        activityType = null,
        saveReason = null,
    )

    private companion object {
        const val Tolerance = 0.000_000_1
    }
}
