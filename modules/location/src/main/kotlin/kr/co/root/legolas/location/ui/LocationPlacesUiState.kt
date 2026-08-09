package kr.co.root.legolas.location.ui

import kr.co.root.legolas.location.data.LocationPlace

data class LocationPlacesUiState(
    val places: List<LocationPlace> = emptyList(),
    val suggestedLatitude: Double? = null,
    val suggestedLongitude: Double? = null,
    val isExternalBasemapEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val hasLoadError: Boolean = false,
    val hasMutationError: Boolean = false,
    val saveCompleted: Boolean = false,
)
