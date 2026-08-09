package kr.co.root.legolas.location.ui

import kr.co.root.legolas.location.data.LocationTimelineEntry
import java.time.LocalDate

data class LocationTimelineUiState(
    val selectedDate: LocalDate = LocalDate.now(SeoulZone),
    val entries: List<LocationTimelineEntry> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
)
