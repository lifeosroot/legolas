package kr.co.root.legolas.location.ui

import kr.co.root.legolas.location.data.LocationSample
import kr.co.root.legolas.location.data.LocationSampleQuality
import java.time.LocalDate

data class LocationRouteUiState(
    val selectedDate: LocalDate = LocalDate.now(SeoulZone),
    val selectedQuality: LocationSampleQuality? = null,
    val samples: List<LocationSample> = emptyList(),
    val isExternalBasemapEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
)
