package kr.co.root.legolas

import org.junit.Assert.assertEquals
import org.junit.Test

class LegolasAppTest {
    @Test
    fun `shows pairing while loading or unpaired`() {
        assertEquals(
            LegolasDestination.Pairing,
            destinationFor(isLoading = true, serverUrl = null, showSettings = false),
        )
        assertEquals(
            LegolasDestination.Pairing,
            destinationFor(isLoading = false, serverUrl = null, showSettings = true),
        )
    }

    @Test
    fun `lands on home when paired`() {
        assertEquals(
            LegolasDestination.Home,
            destinationFor(
                isLoading = false,
                serverUrl = "https://arwen.example.com",
                showSettings = false,
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
                showSettings = true,
            ),
        )
    }
}
