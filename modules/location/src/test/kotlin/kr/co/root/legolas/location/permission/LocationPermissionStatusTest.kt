package kr.co.root.legolas.location.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPermissionStatusTest {
    @Test
    fun `no foreground permission means location is unavailable`() {
        val status = locationPermissionStatus(
            sdkInt = 35,
            hasFineAccess = false,
            hasCoarseAccess = false,
            hasBackgroundAccess = true,
        )

        assertEquals(LocationAccuracy.None, status.accuracy)
        assertFalse(status.hasBackgroundAccess)
        assertFalse(status.canTrackInBackground)
    }

    @Test
    fun `coarse permission is reported as approximate foreground access`() {
        val status = locationPermissionStatus(
            sdkInt = 35,
            hasFineAccess = false,
            hasCoarseAccess = true,
            hasBackgroundAccess = false,
        )

        assertEquals(LocationAccuracy.Approximate, status.accuracy)
        assertTrue(status.hasForegroundAccess)
        assertFalse(status.canTrackInBackground)
    }

    @Test
    fun `approximate location cannot satisfy the fifty meter collection policy`() {
        val status = locationPermissionStatus(
            sdkInt = 35,
            hasFineAccess = false,
            hasCoarseAccess = true,
            hasBackgroundAccess = true,
        )

        assertFalse(status.hasPreciseAccess)
        assertFalse(status.canTrackInBackground)
    }

    @Test
    fun `fine and background permissions allow precise background tracking`() {
        val status = locationPermissionStatus(
            sdkInt = 35,
            hasFineAccess = true,
            hasCoarseAccess = true,
            hasBackgroundAccess = true,
            hasActivityRecognitionAccess = true,
        )

        assertEquals(LocationAccuracy.Precise, status.accuracy)
        assertTrue(status.canTrackInBackground)
    }

    @Test
    fun `activity recognition is required for the full tracking policy on Android 10+`() {
        val status = locationPermissionStatus(
            sdkInt = 35,
            hasFineAccess = true,
            hasCoarseAccess = true,
            hasBackgroundAccess = true,
            hasActivityRecognitionAccess = false,
        )

        assertFalse(status.canTrackInBackground)
    }

    @Test
    fun `foreground permission includes background access before Android 10`() {
        val status = locationPermissionStatus(
            sdkInt = 28,
            hasFineAccess = true,
            hasCoarseAccess = true,
            hasBackgroundAccess = false,
        )

        assertTrue(status.canTrackInBackground)
    }

    @Test
    fun `local network permission is reported without stopping offline collection`() {
        val denied = locationPermissionStatus(
            sdkInt = 37,
            hasFineAccess = true,
            hasCoarseAccess = true,
            hasBackgroundAccess = true,
            hasLocalNetworkAccess = false,
        )

        assertFalse(denied.hasLocalNetworkAccess)
        assertTrue(denied.canTrackInBackground)
        assertTrue(
            locationPermissionStatus(
                sdkInt = 36,
                hasFineAccess = true,
                hasCoarseAccess = true,
                hasBackgroundAccess = true,
                hasLocalNetworkAccess = false,
            ).canTrackInBackground,
        )
    }

    @Test
    fun `disabled device location prevents collection`() {
        val status = locationPermissionStatus(
            sdkInt = 37,
            hasFineAccess = true,
            hasCoarseAccess = true,
            hasBackgroundAccess = true,
            isSystemLocationEnabled = false,
        )

        assertFalse(status.canTrackInBackground)
    }
}
