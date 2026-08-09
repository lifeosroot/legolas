package kr.co.root.legolas.location.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_sample_outbox",
    indices = [Index(value = ["collectedAtMillis"])],
)
data class LocationSampleEntity(
    @PrimaryKey val clientSampleId: String,
    val collectedAtMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyM: Double?,
    val source: String,
    val activityType: String?,
    val saveReason: String?,
)
