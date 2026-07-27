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
    fun `fine and background permissions allow precise background tracking`() {
        val status = locationPermissionStatus(
            sdkInt = 35,
            hasFineAccess = true,
            hasCoarseAccess = true,
            hasBackgroundAccess = true,
        )

        assertEquals(LocationAccuracy.Precise, status.accuracy)
        assertTrue(status.canTrackInBackground)
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
}
