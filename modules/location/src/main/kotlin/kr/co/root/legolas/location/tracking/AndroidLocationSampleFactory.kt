package kr.co.root.legolas.location.tracking

import android.location.Location
import kr.co.root.legolas.location.data.LocationSampleRequest
import javax.inject.Inject

class AndroidLocationSampleFactory @Inject constructor() {
    fun create(
        location: Location,
        source: String,
        activityType: String?,
        saveReason: String? = null,
    ): LocationSampleRequest? {
        if (!location.latitude.isFinite() ||
            !location.longitude.isFinite() ||
            location.latitude !in -90.0..90.0 ||
            location.longitude !in -180.0..180.0 ||
            !location.hasAccuracy() ||
            location.accuracy < 0f
        ) {
            return null
        }
        return LocationSampleRequest(
            collectedAtMillis = location.time,
            latitude = location.latitude,
            longitude = location.longitude,
            horizontalAccuracyM = location.accuracy.toDouble(),
            source = source,
            activityType = activityType,
            saveReason = saveReason,
        )
    }
}
