package kr.co.root.legolas.location.tracking

import java.time.Instant

internal data class RootMovingCandidateSampleDiagnostic(
    val timestamp: Instant?,
    val distanceMeters: Float?,
    val accuracyMeters: Float?,
    val requiredDistanceMeters: Float?,
    val accepted: Boolean,
    val reason: String,
    val movementEvidenceScore: Int,
    val consecutiveMovementSignals: Int,
)
