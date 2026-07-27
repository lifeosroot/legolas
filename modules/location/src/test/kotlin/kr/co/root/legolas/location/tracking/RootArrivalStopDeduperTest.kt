package kr.co.root.legolas.location.tracking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootArrivalStopDeduperTest {

    @Test
    fun blocksArrivalWithinFiveMinutesAndOneHundredMeters() {
        val deduper = RootArrivalStopDeduper()

        assertTrue(deduper.shouldSave(reading(timeMillis = 0L), nowMillis = 0L))
        assertFalse(
            deduper.shouldSave(
                reading(timeMillis = 60_000L, latitude = 37.0008),
                nowMillis = 60_000L,
            ),
        )
    }

    @Test
    fun allowsArrivalBeyondDistanceThreshold() {
        val deduper = RootArrivalStopDeduper()

        assertTrue(deduper.shouldSave(reading(timeMillis = 0L), nowMillis = 0L))
        assertTrue(
            deduper.shouldSave(
                reading(timeMillis = 60_000L, latitude = 37.0010),
                nowMillis = 60_000L,
            ),
        )
    }

    @Test
    fun allowsArrivalAfterFiveMinutes() {
        val deduper = RootArrivalStopDeduper()

        assertTrue(deduper.shouldSave(reading(timeMillis = 0L), nowMillis = 0L))
        assertTrue(
            deduper.shouldSave(
                reading(timeMillis = 6 * 60_000L, latitude = 37.0001),
                nowMillis = 6 * 60_000L,
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
