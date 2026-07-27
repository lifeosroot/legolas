package kr.co.root.legolas.location.tracking

import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

internal class RootMovingCandidateDiagnostics {
    private var active: ActiveCandidate? = null

    fun start(
        nowMillis: Long,
        activityType: String,
        activityConfidences: Map<String, Int>,
    ) {
        active = ActiveCandidate(
            startedAtMillis = nowMillis,
            activityType = activityType,
            activityConfidences = activityConfidences,
        )
    }

    fun recordSample(
        reading: RootLocationReading?,
        evaluation: RootLocationMovementEvaluation,
    ): RootMovingCandidateSampleDiagnostic? {
        val candidate = active ?: return null
        val candidateSampleTimeMillis = reading
            ?.timeMillis
            ?.takeIf { it >= candidate.startedAtMillis }
        candidateSampleTimeMillis?.let(candidate::pruneRecentMovementSignals)
        candidate.sampleCount += 1
        candidate.reasonCounts[evaluation.reason.name] = (candidate.reasonCounts[evaluation.reason.name] ?: 0) + 1
        if (evaluation.accepted && candidateSampleTimeMillis != null) {
            candidate.movementSignalCount += 1
            candidate.recentMovementSignalTimes += candidateSampleTimeMillis
            candidate.pruneRecentMovementSignals(candidateSampleTimeMillis)
        }
        candidate.recentMovementSignalCount = candidate.recentMovementSignalTimes.size
        candidate.maxMovementEvidenceScore = maxOf(
            candidate.maxMovementEvidenceScore,
            evaluation.movementEvidenceScore,
        )
        candidate.maxConsecutiveMovementSignals = maxOf(
            candidate.maxConsecutiveMovementSignals,
            evaluation.consecutiveMovementSignals,
        )

        if (reading != null) {
            val first = candidate.firstReading
            if (first == null) {
                candidate.firstReading = reading
            } else {
                candidate.maxDistanceMeters = maxOf(
                    candidate.maxDistanceMeters,
                    first.distanceMetersTo(reading),
                )
            }

            val previous = candidate.lastReading
            if (previous != null && reading.timeMillis > previous.timeMillis) {
                candidate.totalDistanceMeters += previous.distanceMetersTo(reading)
            }
            if (previous == null || reading.timeMillis > previous.timeMillis) {
                candidate.lastReading = reading
            }
            if (candidateSampleTimeMillis != null) {
                candidate.recordQualifiedReading(reading)
            }
        }

        return RootMovingCandidateSampleDiagnostic(
            timestamp = reading?.timeMillis?.let { Instant.ofEpochMilli(it) },
            distanceMeters = evaluation.distanceMeters,
            accuracyMeters = evaluation.accuracyMeters,
            requiredDistanceMeters = evaluation.requiredDistanceMeters,
            accepted = evaluation.accepted,
            reason = evaluation.reason.name,
            movementEvidenceScore = evaluation.movementEvidenceScore,
            consecutiveMovementSignals = evaluation.consecutiveMovementSignals,
        )
    }

    fun movementSignalCount(): Int = active?.movementSignalCount ?: 0

    fun hasGpsMovementEvidence(
        minimumTotalDistanceMeters: Float,
        minimumMaxDisplacementMeters: Float,
    ): Boolean =
        active?.let { candidate ->
            candidate.movementSignalCount > 0 ||
                candidate.qualifiedTotalDistanceMeters >= minimumTotalDistanceMeters ||
                candidate.qualifiedMaxDisplacementMeters >= minimumMaxDisplacementMeters
        } ?: false

    fun shouldExtendCandidate(
        minimumMovementSignals: Int,
        minimumQualifiedTotalDistanceMeters: Float,
        minimumQualifiedMaxDisplacementMeters: Float,
    ): Boolean =
        active?.let { candidate ->
            candidate.movementSignalCount >= minimumMovementSignals ||
                candidate.qualifiedTotalDistanceMeters >= minimumQualifiedTotalDistanceMeters ||
                candidate.qualifiedMaxDisplacementMeters >= minimumQualifiedMaxDisplacementMeters
        } ?: false

    fun markExtendedByEvidence() {
        active?.candidateExtendedByEvidence = true
    }

    fun isExtendedByEvidence(): Boolean = active?.candidateExtendedByEvidence == true

    fun shouldPromoteToMoving(
        requiredRecentMovementSignals: Int,
        requiredTotalMovementSignals: Int,
        minimumQualifiedTotalDistanceMeters: Float,
        minimumQualifiedMaxDisplacementMeters: Float,
    ): Boolean = promotionReason(
        requiredRecentMovementSignals = requiredRecentMovementSignals,
        requiredTotalMovementSignals = requiredTotalMovementSignals,
        minimumQualifiedTotalDistanceMeters = minimumQualifiedTotalDistanceMeters,
        minimumQualifiedMaxDisplacementMeters = minimumQualifiedMaxDisplacementMeters,
    ) != null

    fun promotionReason(
        requiredRecentMovementSignals: Int,
        requiredTotalMovementSignals: Int,
        minimumQualifiedTotalDistanceMeters: Float,
        minimumQualifiedMaxDisplacementMeters: Float,
    ): String? =
        active?.let { candidate ->
            when {
                candidate.recentMovementSignalCount >= requiredRecentMovementSignals ->
                    PromotionReasonRecentMovementSignals

                candidate.candidateExtendedByEvidence &&
                    candidate.movementSignalCount >= requiredTotalMovementSignals ->
                    PromotionReasonTotalMovementSignals

                candidate.qualifiedTotalDistanceMeters >= minimumQualifiedTotalDistanceMeters &&
                    candidate.qualifiedMaxDisplacementMeters >= minimumQualifiedMaxDisplacementMeters ->
                    PromotionReasonQualifiedDistance

                else -> null
            }
        }

    fun finish(
        nowMillis: Long,
        endReason: String,
        rejectReason: String?,
        promotionReason: String?,
    ): LocationMotionCandidateSummary? {
        val candidate = active ?: return null
        active = null
        val candidateStart = Instant.ofEpochMilli(candidate.startedAtMillis)
        val candidateEnd = Instant.ofEpochMilli(nowMillis)
        return LocationMotionCandidateSummary(
            candidateStart = candidateStart,
            candidateEnd = candidateEnd,
            durationSeconds = Duration.between(candidateStart, candidateEnd).seconds.coerceAtLeast(0),
            sampleCount = candidate.sampleCount,
            movementSignalCount = candidate.movementSignalCount,
            recentMovementSignalCount = candidate.recentMovementSignalCount,
            totalMovementSignalCount = candidate.movementSignalCount,
            maxMovementEvidenceScore = candidate.maxMovementEvidenceScore,
            maxConsecutiveMovementSignals = candidate.maxConsecutiveMovementSignals,
            maxDistanceMeters = candidate.maxDistanceMeters.roundMeters(),
            totalDistanceMeters = candidate.totalDistanceMeters.roundMeters(),
            qualifiedTotalDistanceMeters = candidate.qualifiedTotalDistanceMeters.roundMeters(),
            qualifiedMaxDisplacementMeters = candidate.qualifiedMaxDisplacementMeters.roundMeters(),
            candidateAgeSeconds = Duration.between(candidateStart, candidateEnd).seconds.coerceAtLeast(0),
            candidateExtendedByEvidence = candidate.candidateExtendedByEvidence,
            rejectedJumpCount = candidate.rejectedJumpCount,
            endReason = endReason,
            rejectReason = rejectReason,
            promotionReason = promotionReason,
            activityType = candidate.activityType,
            activityConfidences = candidate.activityConfidences,
            movementReasonCounts = candidate.reasonCounts.toMap(),
        )
    }

    private fun Float.roundMeters(): Float =
        (this * 10).roundToInt() / 10f

    private data class ActiveCandidate(
        val startedAtMillis: Long,
        val activityType: String,
        val activityConfidences: Map<String, Int>,
        var sampleCount: Int = 0,
        var movementSignalCount: Int = 0,
        var recentMovementSignalCount: Int = 0,
        var maxMovementEvidenceScore: Int = 0,
        var maxConsecutiveMovementSignals: Int = 0,
        var maxDistanceMeters: Float = 0f,
        var totalDistanceMeters: Float = 0f,
        var qualifiedTotalDistanceMeters: Float = 0f,
        var qualifiedMaxDisplacementMeters: Float = 0f,
        var rejectedJumpCount: Int = 0,
        var candidateExtendedByEvidence: Boolean = false,
        var firstReading: RootLocationReading? = null,
        var lastReading: RootLocationReading? = null,
        var firstQualifiedReading: RootLocationReading? = null,
        var lastQualifiedReading: RootLocationReading? = null,
        val reasonCounts: MutableMap<String, Int> = linkedMapOf(),
        val recentMovementSignalTimes: ArrayDeque<Long> = ArrayDeque(),
    ) {
        fun pruneRecentMovementSignals(nowMillis: Long) {
            while (
                recentMovementSignalTimes.isNotEmpty() &&
                nowMillis - recentMovementSignalTimes.first() > CandidateRecentMovementSignalWindowMillis
            ) {
                recentMovementSignalTimes.removeFirst()
            }
        }

        fun recordQualifiedReading(reading: RootLocationReading) {
            if (!reading.hasQualifiedAccuracy()) return
            val first = firstQualifiedReading
            if (first == null) {
                firstQualifiedReading = reading
                lastQualifiedReading = reading
                return
            }

            val previous = lastQualifiedReading ?: first
            if (reading.timeMillis <= previous.timeMillis) return

            val distanceMeters = previous.distanceMetersTo(reading)
            val elapsedSeconds = (reading.timeMillis - previous.timeMillis) / 1_000f
            if (elapsedSeconds > 0f && distanceMeters / elapsedSeconds > MaxQualifiedSegmentSpeedMps) {
                rejectedJumpCount += 1
                return
            }

            qualifiedTotalDistanceMeters += distanceMeters
            qualifiedMaxDisplacementMeters = maxOf(
                qualifiedMaxDisplacementMeters,
                first.distanceMetersTo(reading),
            )
            lastQualifiedReading = reading
        }
    }

    private companion object {
        const val PromotionReasonRecentMovementSignals = "RECENT_MOVEMENT_SIGNALS"
        const val PromotionReasonTotalMovementSignals = "TOTAL_MOVEMENT_SIGNALS"
        const val PromotionReasonQualifiedDistance = "QUALIFIED_DISTANCE"
        const val CandidateRecentMovementSignalWindowMillis = 3 * 60_000L
        const val MaxQualifiedSegmentSpeedMps = 35f
    }
}

private fun RootLocationReading.hasQualifiedAccuracy(): Boolean =
    accuracyMeters != null && accuracyMeters >= 0f && accuracyMeters <= 50f

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
