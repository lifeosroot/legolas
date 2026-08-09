package kr.co.root.legolas.location.tracking

data class RootLocationPolicy(
    val motion: RootActivityMotion,
    val priority: Int,
    val intervalMillis: Long,
    val minUpdateIntervalMillis: Long,
    val minUpdateDistanceMeters: Float,
    val waitForAccurateLocation: Boolean,
    val source: String,
)
