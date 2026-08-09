package kr.co.root.legolas.location.tracking

internal enum class RootLocationMovementDecisionReason {
    INVALID_LOCATION,
    ACCURACY_TOO_POOR,
    TIMESTAMP_NOT_INCREASING,
    FIRST_SAMPLE,
    DISTANCE_TOO_SMALL,
    MOVEMENT_SIGNAL,
}
