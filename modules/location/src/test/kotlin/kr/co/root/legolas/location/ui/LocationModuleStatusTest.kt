package kr.co.root.legolas.location.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationModuleStatusTest {
    @Test
    fun `disabled module is off regardless of permission`() {
        assertEquals(
            LocationModuleStatus.Off,
            locationModuleStatus(
                isLoading = false,
                isEnabled = false,
                canTrackInBackground = true,
            ),
        )
    }

    @Test
    fun `enabled module needing permission requires action`() {
        assertEquals(
            LocationModuleStatus.ActionRequired,
            locationModuleStatus(
                isLoading = false,
                isEnabled = true,
                canTrackInBackground = false,
            ),
        )
    }

    @Test
    fun `enabled module with permission is ready`() {
        assertEquals(
            LocationModuleStatus.Ready,
            locationModuleStatus(
                isLoading = false,
                isEnabled = true,
                canTrackInBackground = true,
            ),
        )
    }

    @Test
    fun `collection or upload error requires attention`() {
        assertEquals(
            LocationModuleStatus.ActionRequired,
            locationModuleStatus(
                isLoading = false,
                isEnabled = true,
                canTrackInBackground = true,
                hasError = true,
            ),
        )
    }

    @Test
    fun `enabled local server without Android access requires attention`() {
        assertEquals(
            LocationModuleStatus.ActionRequired,
            locationModuleStatus(
                isLoading = false,
                isEnabled = true,
                canTrackInBackground = true,
                canAccessPairedServer = false,
            ),
        )
    }
}
