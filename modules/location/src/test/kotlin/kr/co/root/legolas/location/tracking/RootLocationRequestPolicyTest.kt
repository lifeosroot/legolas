package kr.co.root.legolas.location.tracking

import com.google.android.gms.location.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RootLocationRequestPolicyTest {

    @Test
    fun movingUsesHighAccuracyAndTenMeterDistance() {
        val policy = RootLocationRequestPolicy.forMotion(RootActivityMotion.Moving)

        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, policy.priority)
        assertEquals(10f, policy.minUpdateDistanceMeters)
        assertEquals("android_activity_high_accuracy", policy.source)
        assertTrue(policy.waitForAccurateLocation)
    }

    @Test
    fun stillUsesFiveMinuteHighAccuracySentinel() {
        val policy = RootLocationRequestPolicy.forMotion(RootActivityMotion.Still)

        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, policy.priority)
        assertEquals(5 * 60_000L, policy.intervalMillis)
        assertEquals(5 * 60_000L, policy.minUpdateIntervalMillis)
        assertEquals(0f, policy.minUpdateDistanceMeters)
        assertTrue(policy.waitForAccurateLocation)
        assertEquals("android_activity_high_accuracy", policy.source)
    }

    @Test
    fun movingCandidateUsesShortHighAccuracyConfirmation() {
        val policy = RootLocationRequestPolicy.forMotion(RootActivityMotion.MovingCandidate)

        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, policy.priority)
        assertEquals(15_000L, policy.intervalMillis)
        assertEquals(5_000L, policy.minUpdateIntervalMillis)
        assertEquals(5f, policy.minUpdateDistanceMeters)
        assertEquals("android_activity_candidate", policy.source)
    }

    @Test
    fun movingDegradedUsesThirtySecondObservation() {
        val policy = RootLocationRequestPolicy.forMotion(RootActivityMotion.MovingDegraded)

        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, policy.priority)
        assertEquals(30_000L, policy.intervalMillis)
        assertEquals(10_000L, policy.minUpdateIntervalMillis)
        assertEquals(10f, policy.minUpdateDistanceMeters)
        assertEquals("android_activity_moving_degraded", policy.source)
    }

    @Test
    fun unknownUsesHighAccuracyEveryFiveMinutes() {
        val policy = RootLocationRequestPolicy.forMotion(RootActivityMotion.Unknown)

        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, policy.priority)
        assertEquals(5 * 60_000L, policy.intervalMillis)
        assertEquals(5 * 60_000L, policy.minUpdateIntervalMillis)
        assertEquals(0f, policy.minUpdateDistanceMeters)
        assertTrue(policy.waitForAccurateLocation)
        assertEquals("android_activity_unknown_high_accuracy", policy.source)
    }

    @Test
    fun inVehicleUsesHighAccuracyOncePerMinute() {
        val policy = RootLocationRequestPolicy.forActivity(RootActivityMotion.Moving, "IN_VEHICLE")

        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, policy.priority)
        assertEquals(60_000L, policy.intervalMillis)
        assertEquals(60_000L, policy.minUpdateIntervalMillis)
        assertEquals(10f, policy.minUpdateDistanceMeters)
        assertEquals("android_activity_high_accuracy", policy.source)
    }

    @Test
    fun movingDegradedIgnoresVehiclePolicy() {
        val policy = RootLocationRequestPolicy.forActivity(RootActivityMotion.MovingDegraded, "IN_VEHICLE")

        assertEquals(30_000L, policy.intervalMillis)
        assertEquals("android_activity_moving_degraded", policy.source)
    }
}
