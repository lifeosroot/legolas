package kr.co.root.legolas.location.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RootLocationBoundarySamplePolicyTest {

    @Test
    fun regularSamplesMustBeNewerThanLastAcceptedSample() {
        val lastAcceptedAt = Instant.parse("2026-06-01T09:05:00Z")

        assertFalse(
            RootLocationBoundarySamplePolicy.shouldPersist(
                collectedAt = Instant.parse("2026-06-01T09:04:00Z"),
                lastAcceptedAt = lastAcceptedAt,
                saveReason = null,
            ),
        )
        assertTrue(
            RootLocationBoundarySamplePolicy.shouldPersist(
                collectedAt = Instant.parse("2026-06-01T09:06:00Z"),
                lastAcceptedAt = lastAcceptedAt,
                saveReason = null,
            ),
        )
    }

    @Test
    fun boundarySamplesCanBeOlderThanLastAcceptedSampleWithoutRewindingIt() {
        val lastAcceptedAt = Instant.parse("2026-06-01T09:05:00Z")
        val moveStartAt = Instant.parse("2026-06-01T09:02:00Z")

        assertTrue(
            RootLocationBoundarySamplePolicy.shouldPersist(
                collectedAt = moveStartAt,
                lastAcceptedAt = lastAcceptedAt,
                saveReason = RootLocationBoundarySamplePolicy.MoveStartSaveReason,
            ),
        )
        assertEquals(
            lastAcceptedAt,
            RootLocationBoundarySamplePolicy.nextLastAcceptedAt(
                current = lastAcceptedAt,
                collectedAt = moveStartAt,
                saveReason = RootLocationBoundarySamplePolicy.MoveStartSaveReason,
            ),
        )
    }
}
