package kr.co.root.legolas.location.ui

data class LocationSettingsUiState(
    val isLoading: Boolean = true,
    val isEnabled: Boolean = false,
    val isExternalBasemapEnabled: Boolean = false,
    val isServiceRunning: Boolean = false,
    val motion: String = "unknown",
    val queuedCount: Int = 0,
    val lastCollectedAtMillis: Long? = null,
    val lastError: String? = null,
)
