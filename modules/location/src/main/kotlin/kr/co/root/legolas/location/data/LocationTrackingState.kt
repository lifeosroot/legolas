package kr.co.root.legolas.location.data

data class LocationTrackingState(
    val enabled: Boolean = false,
    val externalBasemapEnabled: Boolean = false,
    val serviceRunning: Boolean = false,
    val motion: String = "unknown",
    val queuedCount: Int = 0,
    val lastCollectedAtMillis: Long? = null,
    val lastError: String? = null,
)
