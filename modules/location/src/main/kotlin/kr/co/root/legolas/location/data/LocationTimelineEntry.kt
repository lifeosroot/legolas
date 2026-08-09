package kr.co.root.legolas.location.data

import java.time.Instant

data class LocationTimelineEntry(
    val entryType: LocationTimelineEntryType,
    val startedAt: Instant,
    val endedAt: Instant?,
    val placeName: String?,
    val fromPlaceName: String?,
    val toPlaceName: String?,
    val algorithmVersion: String,
)
