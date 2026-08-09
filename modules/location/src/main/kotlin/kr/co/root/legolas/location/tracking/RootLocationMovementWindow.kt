package kr.co.root.legolas.location.tracking

internal data class RootLocationMovementWindow(
    val sampleCount: Int,
    val spanMillis: Long,
    val distanceMeters: Float,
    val startedAtMillis: Long?,
    val endedAtMillis: Long?,
)
