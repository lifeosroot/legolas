package kr.co.root.legolas.location.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RootMovingCandidateDiagnosticsTest {

    @Test
    fun summarizesCandidateWithInsufficientMovementSignals() {
        val gate = RootLocationMotionGate()
        val diagnostics = RootMovingCandidateDiagnostics()
        diagnostics.start(
            nowMillis = 0L,
            activityType = "WALKING",
            activityConfidences = mapOf("WALKING" to 68, "RUNNING" to 0),
        )

        listOf(
            reading(timeMillis = 0L, latitude = 37.0),
            reading(timeMillis = 15_000L, latitude = 37.0003),
            reading(timeMillis = 30_000L, latitude = 37.0006),
        ).forEach { sample ->
            diagnostics.recordSample(
                reading = sample,
                evaluation = gate.onReadingWithDiagnostics(sample).evaluation,
            )
        }

        val summary = diagnostics.finish(
            nowMillis = 86_000L,
            endReason = "MOVING_CANDIDATE_REJECTED",
            rejectReason = "INSUFFICIENT_SIGNAL_COUNT",
            promotionReason = null,
        )!!

        assertEquals(86L, summary.durationSeconds)
        assertEquals(86L, summary.candidateAgeSeconds)
        assertEquals(3, summary.sampleCount)
        assertEquals(2, summary.movementSignalCount)
        assertEquals(2, summary.totalMovementSignalCount)
        assertEquals(2, summary.maxMovementEvidenceScore)
        assertEquals(2, summary.maxConsecutiveMovementSignals)
        assertTrue(summary.maxDistanceMeters > 60f)
        assertTrue(summary.totalDistanceMeters > 60f)
        assertEquals("INSUFFICIENT_SIGNAL_COUNT", summary.rejectReason)
        assertEquals(1, summary.movementReasonCounts[RootLocationMovementDecisionReason.FIRST_SAMPLE.name])
        assertEquals(2, summary.movementReasonCounts[RootLocationMovementDecisionReason.MOVEMENT_SIGNAL.name])
    }

    @Test
    fun promotesCandidateWhenMovementSignalsReachThresholdAcrossWholeCandidate() {
        val gate = RootLocationMotionGate()
        val diagnostics = RootMovingCandidateDiagnostics()
        diagnostics.start(
            nowMillis = 0L,
            activityType = "WALKING",
            activityConfidences = mapOf("WALKING" to 92),
        )

        listOf(
            reading(timeMillis = 0L, latitude = 37.0),
            reading(timeMillis = 15_000L, latitude = 37.0003),
            reading(timeMillis = 30_000L, latitude = 37.0006),
            reading(timeMillis = 211_000L, latitude = 37.0010),
        ).forEach { sample ->
            diagnostics.recordSample(
                reading = sample,
                evaluation = gate.onReadingWithDiagnostics(sample).evaluation,
            )
        }

        assertEquals(
            false,
            diagnostics.shouldPromoteToMoving(
                requiredRecentMovementSignals = 3,
                requiredTotalMovementSignals = 3,
                minimumQualifiedTotalDistanceMeters = 1_000f,
                minimumQualifiedMaxDisplacementMeters = 1_000f,
            ),
        )
        diagnostics.markExtendedByEvidence()
        assertEquals(
            true,
            diagnostics.shouldPromoteToMoving(
                requiredRecentMovementSignals = 3,
                requiredTotalMovementSignals = 3,
                minimumQualifiedTotalDistanceMeters = 1_000f,
                minimumQualifiedMaxDisplacementMeters = 1_000f,
            ),
        )
        val summary = diagnostics.finish(
            nowMillis = 211_000L,
            endReason = "GPS_MOVING",
            rejectReason = null,
            promotionReason = "TOTAL_MOVEMENT_SIGNALS",
        )!!

        assertEquals(3, summary.movementSignalCount)
        assertEquals(2, summary.maxMovementEvidenceScore)
        assertEquals(true, summary.candidateExtendedByEvidence)
        assertEquals("TOTAL_MOVEMENT_SIGNALS", summary.promotionReason)
    }

    @Test
    fun doesNotPromoteFromMovementSignalsRecordedBeforeCandidateStart() {
        val gate = RootLocationMotionGate()
        gate.onReadingWithDiagnostics(reading(timeMillis = 0L, latitude = 37.0))
        gate.onReadingWithDiagnostics(reading(timeMillis = 15_000L, latitude = 37.0003))
        gate.onReadingWithDiagnostics(reading(timeMillis = 30_000L, latitude = 37.0006))

        val diagnostics = RootMovingCandidateDiagnostics()
        diagnostics.start(
            nowMillis = 45_000L,
            activityType = "WALKING",
            activityConfidences = mapOf("WALKING" to 92),
        )

        val candidateReading = reading(timeMillis = 45_000L, latitude = 37.0009)
        val candidateEvaluation = gate.onReadingWithDiagnostics(candidateReading).evaluation
        assertEquals(3, candidateEvaluation.movementEvidenceScore)
        diagnostics.recordSample(
            reading = candidateReading,
            evaluation = candidateEvaluation,
        )

        assertEquals(
            false,
            diagnostics.shouldPromoteToMoving(
                requiredRecentMovementSignals = 3,
                requiredTotalMovementSignals = 3,
                minimumQualifiedTotalDistanceMeters = 1_000f,
                minimumQualifiedMaxDisplacementMeters = 1_000f,
            ),
        )
        val summary = diagnostics.finish(
            nowMillis = 45_000L,
            endReason = "MOVING_CANDIDATE_REJECTED",
            rejectReason = "INSUFFICIENT_SIGNAL_COUNT",
            promotionReason = null,
        )!!

        assertEquals(1, summary.sampleCount)
        assertEquals(1, summary.movementSignalCount)
        assertEquals(1, summary.recentMovementSignalCount)
        assertEquals(3, summary.maxMovementEvidenceScore)
    }

    @Test
    fun promotesCandidateWhenQualifiedDistancePassesThreshold() {
        val gate = RootLocationMotionGate()
        val diagnostics = RootMovingCandidateDiagnostics()
        diagnostics.start(
            nowMillis = 0L,
            activityType = "WALKING",
            activityConfidences = mapOf("WALKING" to 92),
        )

        listOf(
            reading(timeMillis = 0L, latitude = 37.0),
            reading(timeMillis = 15_000L, latitude = 37.00018),
            reading(timeMillis = 30_000L, latitude = 37.00036),
            reading(timeMillis = 45_000L, latitude = 37.00054),
            reading(timeMillis = 60_000L, latitude = 37.00072),
            reading(timeMillis = 75_000L, latitude = 37.00090),
        ).forEach { sample ->
            diagnostics.recordSample(
                reading = sample,
                evaluation = gate.onReadingWithDiagnostics(sample).evaluation,
            )
        }

        assertEquals(
            true,
            diagnostics.shouldPromoteToMoving(
                requiredRecentMovementSignals = 3,
                requiredTotalMovementSignals = 3,
                minimumQualifiedTotalDistanceMeters = 100f,
                minimumQualifiedMaxDisplacementMeters = 70f,
            ),
        )
        assertEquals(
            true,
            diagnostics.hasGpsMovementEvidence(
                minimumTotalDistanceMeters = 50f,
                minimumMaxDisplacementMeters = 40f,
            ),
        )
    }

    @Test
    fun excludesRejectedJumpsFromQualifiedDistance() {
        val gate = RootLocationMotionGate()
        val diagnostics = RootMovingCandidateDiagnostics()
        diagnostics.start(
            nowMillis = 0L,
            activityType = "WALKING",
            activityConfidences = mapOf("WALKING" to 92),
        )

        listOf(
            reading(timeMillis = 0L, latitude = 37.0),
            reading(timeMillis = 1_000L, latitude = 37.01),
        ).forEach { sample ->
            diagnostics.recordSample(
                reading = sample,
                evaluation = gate.onReadingWithDiagnostics(sample).evaluation,
            )
        }

        val summary = diagnostics.finish(
            nowMillis = 1_000L,
            endReason = "MOVING_CANDIDATE_REJECTED",
            rejectReason = "TIMEOUT",
            promotionReason = null,
        )!!

        assertEquals(1, summary.rejectedJumpCount)
        assertEquals(0f, summary.qualifiedTotalDistanceMeters)
    }

    private fun reading(
        timeMillis: Long,
        latitude: Double = 37.0,
        longitude: Double = 127.0,
        accuracyMeters: Float = 12f,
    ): RootLocationReading =
        RootLocationReading(
            timeMillis = timeMillis,
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters,
            speedMps = null,
        )
}
