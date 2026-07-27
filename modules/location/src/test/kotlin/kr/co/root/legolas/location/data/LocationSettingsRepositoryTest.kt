package kr.co.root.legolas.location.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationSettingsRepositoryTest {
    @Test
    fun `backdated boundary sample does not rewind last collected time`() {
        assertEquals(2_000L, latestCollectedAt(previous = 2_000L, candidate = 1_000L))
    }

    @Test
    fun `newer sample advances last collected time`() {
        assertEquals(2_000L, latestCollectedAt(previous = 1_000L, candidate = 2_000L))
        assertEquals(2_000L, latestCollectedAt(previous = null, candidate = 2_000L))
    }
}
