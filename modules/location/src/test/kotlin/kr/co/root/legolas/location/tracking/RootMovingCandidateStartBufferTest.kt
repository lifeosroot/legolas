package kr.co.root.legolas.location.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RootMovingCandidateStartBufferTest {

    @Test
    fun bestReturnsEarliestPreferredAccuracyCandidateStart() {
        val buffer = RootMovingCandidateStartBuffer<String>()

        buffer.record(value = "first", timeMillis = 0L, accuracyMeters = 15f)
        buffer.record(value = "best", timeMillis = 30_000L, accuracyMeters = 5f)
        buffer.record(value = "latest", timeMillis = 60_000L, accuracyMeters = 10f)

        assertEquals("first", buffer.best())
    }

    @Test
    fun bestFallsBackToMostAccurateCandidateWhenNoPreferredAccuracyExists() {
        val buffer = RootMovingCandidateStartBuffer<String>()

        buffer.record(value = "earlier", timeMillis = 0L, accuracyMeters = 45f)
        buffer.record(value = "best", timeMillis = 30_000L, accuracyMeters = 38f)
        buffer.record(value = "latest", timeMillis = 60_000L, accuracyMeters = 42f)

        assertEquals("best", buffer.best())
    }

    @Test
    fun bestUsesOlderCandidateWhenAccuracyTies() {
        val buffer = RootMovingCandidateStartBuffer<String>()

        buffer.record(value = "older", timeMillis = 0L, accuracyMeters = 8f)
        buffer.record(value = "newer", timeMillis = 30_000L, accuracyMeters = 8f)

        assertEquals("older", buffer.best())
    }

    @Test
    fun recordIgnoresPoorAccuracyCandidates() {
        val buffer = RootMovingCandidateStartBuffer<String>()

        buffer.record(value = "poor", timeMillis = 0L, accuracyMeters = 80f)
        buffer.record(value = "missing", timeMillis = 30_000L, accuracyMeters = null)

        assertNull(buffer.best())
    }

    @Test
    fun recordDropsCandidatesOutsideRecentWindow() {
        val buffer = RootMovingCandidateStartBuffer<String>()

        buffer.record(value = "old", timeMillis = 0L, accuracyMeters = 5f)
        buffer.record(value = "recent", timeMillis = 121_000L, accuracyMeters = 10f)

        assertEquals("recent", buffer.best())
    }
}
