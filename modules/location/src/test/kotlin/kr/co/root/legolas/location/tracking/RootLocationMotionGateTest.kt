package kr.co.root.legolas.location.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RootLocationMotionGateTest {

    @Test
    fun ignoresGpsJitterWithinAccuracyRadius() {
        val gate = RootLocationMotionGate()

        assertNull(gate.onReading(reading(timeMillis = 0L, latitude = 37.0, longitude = 127.0)))
        assertNull(gate.onReading(reading(timeMillis = 150_000L, latitude = 37.00005, longitude = 127.00005)))

        assertEquals(
            RootActivityMotion.Still,
            gate.onReading(reading(timeMillis = 300_000L, latitude = 37.00004, longitude = 127.00003)),
        )
    }

    @Test
    fun returnsMovingAfterThreeSignalsInEvidenceWindow() {
        val gate = RootLocationMotionGate()

        assertNull(gate.onReading(reading(timeMillis = 0L, latitude = 37.0)))
        assertNull(gate.onReading(reading(timeMillis = 15_000L, latitude = 37.0003)))
        assertNull(gate.onReading(reading(timeMillis = 30_000L, latitude = 37.0006)))

        assertEquals(
            RootActivityMotion.Moving,
            gate.onReading(reading(timeMillis = 45_000L, latitude = 37.0009)),
        )
    }

    @Test
    fun singleFailedSignalDoesNotResetMovementEvidence() {
        val gate = RootLocationMotionGate()

        assertNull(gate.onReadingWithDiagnostics(reading(timeMillis = 0L, latitude = 37.0)).motion)
        assertNull(gate.onReadingWithDiagnostics(reading(timeMillis = 15_000L, latitude = 37.0003)).motion)
        assertNull(gate.onReadingWithDiagnostics(reading(timeMillis = 30_000L, latitude = 37.0006)).motion)

        val failedSignal = gate.onReadingWithDiagnostics(reading(timeMillis = 45_000L, latitude = 37.00061))
        assertNull(failedSignal.motion)
        assertEquals(2, failedSignal.evaluation.movementEvidenceScore)
        assertEquals(0, failedSignal.evaluation.consecutiveMovementSignals)

        val recoveredSignal = gate.onReadingWithDiagnostics(reading(timeMillis = 60_000L, latitude = 37.00091))
        assertEquals(RootActivityMotion.Moving, recoveredSignal.motion)
        assertEquals(3, recoveredSignal.evaluation.movementEvidenceScore)
        assertEquals(1, recoveredSignal.evaluation.consecutiveMovementSignals)
    }

    @Test
    fun speedWithoutDisplacementDoesNotCreateMovingSignal() {
        val gate = RootLocationMotionGate()

        gate.onReading(reading(timeMillis = 0L, speedMps = 1.2f))
        assertNull(gate.onReading(reading(timeMillis = 15_000L, speedMps = 1.2f)))
        assertNull(gate.onReading(reading(timeMillis = 30_000L, speedMps = 1.2f)))
        assertNull(gate.onReading(reading(timeMillis = 45_000L, speedMps = 1.2f)))
    }

    @Test
    fun oldMovingSignalsDoNotBlockStableStillWindow() {
        val gate = RootLocationMotionGate()

        gate.onReading(reading(timeMillis = 0L, latitude = 37.0))
        gate.onReading(reading(timeMillis = 15_000L, latitude = 37.0003))
        gate.onReading(reading(timeMillis = 30_000L, latitude = 37.0006))
        gate.onReading(reading(timeMillis = 45_000L, latitude = 37.0009))

        assertNull(gate.onReading(reading(timeMillis = 90_000L, latitude = 37.001, longitude = 127.00002)))
        assertNull(gate.onReading(reading(timeMillis = 240_000L, latitude = 37.00103, longitude = 127.00001)))

        assertEquals(
            RootActivityMotion.Still,
            gate.onReading(reading(timeMillis = 390_000L, latitude = 37.00101, longitude = 127.00003)),
        )
    }

    @Test
    fun stableStillWindowAllowsSlightlyShortCoverage() {
        val gate = RootLocationMotionGate()

        assertNull(gate.onReading(reading(timeMillis = 0L, latitude = 37.0, longitude = 127.0)))
        assertNull(gate.onReading(reading(timeMillis = 120_000L, latitude = 37.00002, longitude = 127.00002)))

        assertEquals(
            RootActivityMotion.Still,
            gate.onReading(reading(timeMillis = 240_000L, latitude = 37.00001, longitude = 127.00001)),
        )
    }

    @Test
    fun stableStillWindowExposesStartTimestamp() {
        val gate = RootLocationMotionGate()

        gate.onReading(reading(timeMillis = 0L, latitude = 37.0, longitude = 127.0))
        gate.onReading(reading(timeMillis = 120_000L, latitude = 37.00002, longitude = 127.00002))
        gate.onReading(reading(timeMillis = 240_000L, latitude = 37.00001, longitude = 127.00001))

        assertEquals(0L, gate.stableStillWindowStartMillis(nowMillis = 240_000L))
    }

    @Test
    fun doesNotReturnStillWhenRecentLocationsKeepSpreading() {
        val gate = RootLocationMotionGate()

        assertNull(gate.onReading(reading(timeMillis = 0L, latitude = 37.0)))
        assertNull(gate.onReading(reading(timeMillis = 150_000L, latitude = 37.0008)))
        assertNull(gate.onReading(reading(timeMillis = 300_000L, latitude = 37.0016)))
    }

    @Test
    fun badAccuracySamplesAreExcludedFromStillWindow() {
        val gate = RootLocationMotionGate()

        assertNull(gate.onReading(reading(timeMillis = 0L, accuracyMeters = 80f)))
        assertNull(gate.onReading(reading(timeMillis = 60_000L)))
        assertNull(gate.onReading(reading(timeMillis = 180_000L)))

        assertEquals(
            RootActivityMotion.Still,
            gate.onReading(reading(timeMillis = 300_000L)),
        )
    }

    @Test
    fun candidateStillWindowAcceptsShortIndoorJitter() {
        val gate = RootLocationMotionGate()

        gate.onReading(reading(timeMillis = 0L, latitude = 37.0, longitude = 127.0))
        gate.onReading(reading(timeMillis = 60_000L, latitude = 37.0002, longitude = 127.0001))
        gate.onReading(reading(timeMillis = 120_000L, latitude = 36.9999, longitude = 127.0002))

        assertEquals(true, gate.hasCandidateStillWindow(nowMillis = 120_000L))
    }

    @Test
    fun recordsLastMeaningfulMovementTimestamp() {
        val gate = RootLocationMotionGate()

        gate.onReading(reading(timeMillis = 0L, latitude = 37.0))
        gate.onReading(reading(timeMillis = 15_000L, latitude = 37.0003))

        assertEquals(15_000L, gate.lastMeaningfulMovementAtMillis())
    }

    @Test
    fun recentMovementSignalCountCanUseShorterWindow() {
        val gate = RootLocationMotionGate()

        gate.onReading(reading(timeMillis = 0L, latitude = 37.0))
        gate.onReading(reading(timeMillis = 15_000L, latitude = 37.0003))
        gate.onReading(reading(timeMillis = 30_000L, latitude = 37.0006))

        assertEquals(2, gate.recentMovementSignalCount(nowMillis = 151_000L))
        assertEquals(
            0,
            gate.recentMovementSignalCount(
                nowMillis = 151_000L,
                windowMillis = 2 * 60_000L,
            ),
        )
    }

    @Test
    fun recentMovementWindowUsesOnlyAccurateSamples() {
        val gate = RootLocationMotionGate()

        gate.onReading(reading(timeMillis = 0L, latitude = 37.0, accuracyMeters = 12f))
        gate.onReading(reading(timeMillis = 60_000L, latitude = 37.0001, accuracyMeters = 80f))
        gate.onReading(reading(timeMillis = 120_000L, latitude = 37.0002, accuracyMeters = 12f))

        val window = gate.recentMovementWindow(
            nowMillis = 120_000L,
            windowMillis = 5 * 60_000L,
            maxAccuracyMeters = 50f,
        )

        assertEquals(2, window.sampleCount)
        assertEquals(120_000L, window.spanMillis)
        assertEquals(0L, window.startedAtMillis)
        assertEquals(120_000L, window.endedAtMillis)
        assertEquals(true, window.distanceMeters > 20f)
    }

    @Test
    fun movementEvaluationExplainsRejectedDistance() {
        val gate = RootLocationMotionGate()

        assertEquals(
            RootLocationMovementDecisionReason.FIRST_SAMPLE,
            gate.onReadingWithDiagnostics(reading(timeMillis = 0L, latitude = 37.0)).evaluation.reason,
        )
        val result = gate.onReadingWithDiagnostics(reading(timeMillis = 15_000L, latitude = 37.0001))

        assertEquals(false, result.evaluation.accepted)
        assertEquals(RootLocationMovementDecisionReason.DISTANCE_TOO_SMALL, result.evaluation.reason)
        assertEquals(25f, result.evaluation.requiredDistanceMeters)
    }

    @Test
    fun movementEvaluationExplainsPoorAccuracy() {
        val gate = RootLocationMotionGate()

        val result = gate.onReadingWithDiagnostics(reading(timeMillis = 0L, accuracyMeters = 55f))

        assertNull(result.motion)
        assertEquals(false, result.evaluation.accepted)
        assertEquals(RootLocationMovementDecisionReason.ACCURACY_TOO_POOR, result.evaluation.reason)
    }

    private fun reading(
        timeMillis: Long,
        latitude: Double = 37.0,
        longitude: Double = 127.0,
        accuracyMeters: Float = 12f,
        speedMps: Float? = null,
    ): RootLocationReading =
        RootLocationReading(
            timeMillis = timeMillis,
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters,
            speedMps = speedMps,
        )
}
