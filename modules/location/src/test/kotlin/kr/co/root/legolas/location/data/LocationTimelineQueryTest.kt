package kr.co.root.legolas.location.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LocationTimelineQueryTest {
    @Test
    fun `query path includes selected date`() {
        assertEquals(
            "/api/location/timeline?date=2026-07-27",
            locationTimelinePath(LocalDate.of(2026, 7, 27)),
        )
    }

}
