package kr.co.root.legolas.location.tracking

import java.time.Instant

internal data class LocationMotionCandidateSummary(
    val candidateStart: Instant,
    val candidateEnd: Instant,
    val durationSeconds: Long,
    val sampleCount: Int,
    val movementSignalCount: Int,
    val recentMovementSignalCount: Int,
    val totalMovementSignalCount: Int,
    val maxMovementEvidenceScore: Int,
    val maxConsecutiveMovementSignals: Int,
    val maxDistanceMeters: Float,
    val totalDistanceMeters: Float,
    val qualifiedTotalDistanceMeters: Float,
    val qualifiedMaxDisplacementMeters: Float,
    val candidateAgeSeconds: Long,
    val candidateExtendedByEvidence: Boolean,
    val rejectedJumpCount: Int,
    val endReason: String,
    val rejectReason: String?,
    val promotionReason: String?,
    val activityType: String,
    val activityConfidences: Map<String, Int>,
    val movementReasonCounts: Map<String, Int>,
)
