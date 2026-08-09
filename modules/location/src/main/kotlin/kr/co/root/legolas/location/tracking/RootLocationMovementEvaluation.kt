package kr.co.root.legolas.location.tracking

internal data class RootLocationMovementEvaluation(
    val reading: RootLocationReading?,
    val distanceMeters: Float?,
    val accuracyMeters: Float?,
    val requiredDistanceMeters: Float?,
    val accepted: Boolean,
    val reason: RootLocationMovementDecisionReason,
    val movementEvidenceScore: Int,
    val consecutiveMovementSignals: Int,
)
