package kr.co.root.legolas.location.data

import java.util.UUID

data class LocationSampleRequest(
    val clientSampleId: String = UUID.randomUUID().toString(),
    val collectedAtMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyM: Double?,
    val source: String,
    val activityType: String?,
    val saveReason: String? = null,
)
