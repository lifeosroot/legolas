package kr.co.root.legolas.location.data

import java.time.Instant

data class LocationSample(
    val id: Long,
    val collectedAt: Instant,
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyM: Double?,
    val source: String,
    val quality: LocationSampleQuality,
    val activityType: String?,
    val saveReason: String?,
)
