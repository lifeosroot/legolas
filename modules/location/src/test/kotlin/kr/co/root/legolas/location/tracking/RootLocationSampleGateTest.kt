package kr.co.root.legolas.location.tracking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootLocationSampleGateTest {

    @Test
    fun movingSamplesRequireIntervalAndMeaningfulDistance() {
        val gate = RootLocationSampleGate()

        assertTrue(gate.shouldAccept(reading(timeMillis = 0L), RootActivityMotion.Moving))
        assertFalse(
            gate.shouldAccept(
                reading(timeMillis = 5_000L, latitude = 37.0003),
                RootActivityMotion.Moving,
            ),
        )
        assertTrue(
            gate.shouldAccept(
                reading(timeMillis = 15_000L, latitude = 37.0003),
                RootActivityMotion.Moving,
            ),
        )
    }

    @Test
    fun stillSamplesAreThrottledToFifteenMinuteHeartbeat() {
        val gate = RootLocationSampleGate()

        assertTrue(gate.shouldAccept(reading(timeMillis = 0L), RootActivityMotion.Still))
        assertFalse(gate.shouldAccept(reading(timeMillis = 5 * 60_000L), RootActivityMotion.Still))
        assertTrue(gate.shouldAccept(reading(timeMillis = 15 * 60_000L), RootActivityMotion.Still))
    }

    @Test
    fun movingCandidateSamplesAreNotPersisted() {
        val gate = RootLocationSampleGate()

        assertFalse(gate.shouldAccept(reading(timeMillis = 0L), RootActivityMotion.MovingCandidate))
        assertFalse(
            gate.shouldAccept(
                reading(timeMillis = 60_000L, latitude = 37.0003),
                RootActivityMotion.MovingCandidate,
            ),
        )
    }

    @Test
    fun movingDegradedSamplesAreNotPersisted() {
        val gate = RootLocationSampleGate()

        assertFalse(gate.shouldAccept(reading(timeMillis = 0L), RootActivityMotion.MovingDegraded))
        assertFalse(
            gate.shouldAccept(
                reading(timeMillis = 60_000L, latitude = 37.0003),
                RootActivityMotion.MovingDegraded,
            ),
        )
    }

    @Test
    fun unknownSamplesUseFifteenMinuteHeartbeat() {
        val gate = RootLocationSampleGate()

        assertTrue(gate.shouldAccept(reading(timeMillis = 0L), RootActivityMotion.Unknown))
        assertFalse(gate.shouldAccept(reading(timeMillis = 5 * 60_000L), RootActivityMotion.Unknown))
        assertTrue(gate.shouldAccept(reading(timeMillis = 15 * 60_000L), RootActivityMotion.Unknown))
    }

    @Test
    fun badAccuracySamplesAreDropped() {
        val gate = RootLocationSampleGate()

        assertFalse(
            gate.shouldAccept(
                reading(timeMillis = 0L, accuracyMeters = 80f),
                RootActivityMotion.Moving,
            ),
        )
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
