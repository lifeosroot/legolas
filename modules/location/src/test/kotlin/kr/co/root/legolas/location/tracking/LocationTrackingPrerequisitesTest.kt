package kr.co.root.legolas.location.tracking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationTrackingPrerequisitesTest {
    @Test
    fun `stale service intent cannot restart an opted out module`() {
        assertFalse(canStart(enabled = false))
    }

    @Test
    fun `tracking starts only when every collection prerequisite is present`() {
        assertTrue(canStart(enabled = true))
        assertFalse(canStart(enabled = true, hasPreciseLocation = false))
        assertFalse(canStart(enabled = true, hasBackgroundLocation = false))
        assertFalse(canStart(enabled = true, hasActivityRecognition = false))
        assertFalse(canStart(enabled = true, hasNotifications = false))
        assertFalse(canStart(enabled = true, isSystemLocationEnabled = false))
    }

    private fun canStart(
        enabled: Boolean,
        hasPreciseLocation: Boolean = true,
        hasBackgroundLocation: Boolean = true,
        hasActivityRecognition: Boolean = true,
        hasNotifications: Boolean = true,
        isSystemLocationEnabled: Boolean = true,
    ): Boolean = canStartLocationTracking(
        enabled = enabled,
        hasPreciseLocation = hasPreciseLocation,
        hasBackgroundLocation = hasBackgroundLocation,
        hasActivityRecognition = hasActivityRecognition,
        hasNotifications = hasNotifications,
        isSystemLocationEnabled = isSystemLocationEnabled,
    )
}
