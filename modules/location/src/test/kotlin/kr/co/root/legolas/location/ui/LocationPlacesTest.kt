package kr.co.root.legolas.location.ui

import kr.co.root.legolas.location.data.LocationPlaceDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationPlacesTest {
    @Test
    fun `valid editor values create normalized draft`() {
        assertEquals(
            LocationPlaceDraft(
                name = "Home",
                latitude = 37.5665,
                longitude = 126.978,
                radiusMeters = 50.0,
            ),
            locationPlaceDraftOrNull("  Home  ", "37.5665", "126.978", "50"),
        )
    }

    @Test
    fun `editor rejects values outside server contract`() {
        assertNull(locationPlaceDraftOrNull("", "37", "127", "50"))
        assertNull(locationPlaceDraftOrNull("Home", "91", "127", "50"))
        assertNull(locationPlaceDraftOrNull("Home", "37", "181", "50"))
        assertNull(locationPlaceDraftOrNull("Home", "37", "127", "0"))
        assertNull(locationPlaceDraftOrNull("Home", "37", "127", "10001"))
    }
}
