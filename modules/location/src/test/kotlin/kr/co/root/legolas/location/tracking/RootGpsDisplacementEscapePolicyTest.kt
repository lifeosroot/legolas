package kr.co.root.legolas.location.tracking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootGpsDisplacementEscapePolicyTest {

    @Test
    fun stillEscapesToMovingWhenAccurateGpsMovesHundredsOfMeters() {
        val result = movementResult(
            currentLatitude = 37.004,
            currentAccuracyMeters = 12f,
        )

        assertTrue(
            RootGpsDisplacementEscapePolicy.shouldEscape(
                currentMotion = RootActivityMotion.Still,
                evaluation = result.evaluation,
            ),
        )
    }

    @Test
    fun unknownEscapesToMovingWhenAccurateGpsMovesHundredsOfMeters() {
        val result = movementResult(
            currentLatitude = 37.004,
            currentAccuracyMeters = 12f,
        )

        assertTrue(
            RootGpsDisplacementEscapePolicy.shouldEscape(
                currentMotion = RootActivityMotion.Unknown,
                evaluation = result.evaluation,
            ),
        )
    }

    @Test
    fun smallDisplacementDoesNotEscapeStill() {
        val result = movementResult(
            currentLatitude = 37.001,
            currentAccuracyMeters = 12f,
        )

        assertFalse(
            RootGpsDisplacementEscapePolicy.shouldEscape(
                currentMotion = RootActivityMotion.Still,
                evaluation = result.evaluation,
            ),
        )
    }

    @Test
    fun poorAccuracyDoesNotEscapeStill() {
        val result = movementResult(
            currentLatitude = 37.004,
            currentAccuracyMeters = 80f,
        )

        assertFalse(
            RootGpsDisplacementEscapePolicy.shouldEscape(
                currentMotion = RootActivityMotion.Still,
                evaluation = result.evaluation,
            ),
        )
    }

    @Test
    fun movingStateDoesNotUseStillEscape() {
        val result = movementResult(
            currentLatitude = 37.004,
            currentAccuracyMeters = 12f,
        )

        assertFalse(
            RootGpsDisplacementEscapePolicy.shouldEscape(
                currentMotion = RootActivityMotion.Moving,
                evaluation = result.evaluation,
            ),
        )
    }

    private fun movementResult(
        currentLatitude: Double,
        currentAccuracyMeters: Float,
    ): RootLocationMotionResult {
        val gate = RootLocationMotionGate()
        gate.onReadingWithDiagnostics(
            reading(
                timeMillis = 0L,
                latitude = 37.0,
                accuracyMeters = 12f,
            ),
        )
        return gate.onReadingWithDiagnostics(
            reading(
                timeMillis = 15 * 60_000L,
                latitude = currentLatitude,
                accuracyMeters = currentAccuracyMeters,
            ),
        )
    }

    private fun reading(
        timeMillis: Long,
        latitude: Double,
        accuracyMeters: Float,
    ): RootLocationReading =
        RootLocationReading(
            timeMillis = timeMillis,
            latitude = latitude,
            longitude = 127.0,
            accuracyMeters = accuracyMeters,
            speedMps = null,
        )
}
