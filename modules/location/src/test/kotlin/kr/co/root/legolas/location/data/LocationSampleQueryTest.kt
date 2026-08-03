package kr.co.root.legolas.location.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LocationSampleQueryTest {
    @Test
    fun `query path includes date and optional quality`() {
        val date = LocalDate.of(2026, 7, 27)

        assertEquals(
            "/api/location/samples?date=2026-07-27",
            locationSamplesPath(date, null),
        )
        assertEquals(
            "/api/location/samples?date=2026-07-27&quality=GOOD",
            locationSamplesPath(date, LocationSampleQuality.GOOD),
        )
    }
}
