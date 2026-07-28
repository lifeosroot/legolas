package kr.co.root.legolas

import org.junit.Assert.assertEquals
import org.junit.Test

class LegolasAppTest {
    @Test
    fun `shows pairing while loading or unpaired`() {
        assertEquals(
            LegolasDestination.Pairing,
            destinationFor(
                isLoading = true,
                serverUrl = null,
                requestedDestination = LegolasDestination.Home,
            ),
        )
        assertEquals(
            LegolasDestination.Pairing,
            destinationFor(
                isLoading = false,
                serverUrl = null,
                requestedDestination = LegolasDestination.Settings,
            ),
        )
    }

    @Test
    fun `lands on home when paired`() {
        assertEquals(
            LegolasDestination.Home,
            destinationFor(
                isLoading = false,
                serverUrl = "https://arwen.example.com",
                requestedDestination = LegolasDestination.Home,
            ),
        )
    }

    @Test
    fun `shows settings when requested while paired`() {
        assertEquals(
            LegolasDestination.Settings,
            destinationFor(
                isLoading = false,
                serverUrl = "https://arwen.example.com",
                requestedDestination = LegolasDestination.Settings,
            ),
        )
    }

    @Test
    fun `shows location when requested while paired`() {
        assertEquals(
            LegolasDestination.Location,
            destinationFor(
                isLoading = false,
                serverUrl = "https://arwen.example.com",
                requestedDestination = LegolasDestination.Location,
            ),
        )
    }

    @Test
    fun `returns home when location is excluded from the build`() {
        assertEquals(
            LegolasDestination.Home,
            destinationFor(
                isLoading = false,
                serverUrl = "https://arwen.example.com",
                requestedDestination = LegolasDestination.Location,
                isLocationAvailable = false,
            ),
        )
    }
}
