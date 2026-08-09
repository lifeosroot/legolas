package kr.co.root.legolas.location.tracking

import com.google.android.gms.location.Priority

object RootLocationRequestPolicy {
    fun forActivity(motion: RootActivityMotion, activityType: String?): RootLocationPolicy =
        if (motion == RootActivityMotion.Moving && activityType == "IN_VEHICLE") {
            vehicleHighAccuracy(motion)
        } else {
            forMotion(motion)
        }

    fun forMotion(motion: RootActivityMotion): RootLocationPolicy =
        when (motion) {
            RootActivityMotion.Moving -> RootLocationPolicy(
                motion = motion,
                priority = Priority.PRIORITY_HIGH_ACCURACY,
                intervalMillis = 15_000L,
                minUpdateIntervalMillis = 5_000L,
                minUpdateDistanceMeters = 10f,
                waitForAccurateLocation = true,
                source = "android_activity_high_accuracy",
            )

            RootActivityMotion.MovingDegraded -> RootLocationPolicy(
                motion = motion,
                priority = Priority.PRIORITY_HIGH_ACCURACY,
                intervalMillis = 30_000L,
                minUpdateIntervalMillis = 10_000L,
                minUpdateDistanceMeters = 10f,
                waitForAccurateLocation = true,
                source = "android_activity_moving_degraded",
            )

            RootActivityMotion.Still -> RootLocationPolicy(
                motion = motion,
                priority = Priority.PRIORITY_HIGH_ACCURACY,
                intervalMillis = 5 * 60_000L,
                minUpdateIntervalMillis = 5 * 60_000L,
                minUpdateDistanceMeters = 0f,
                waitForAccurateLocation = true,
                source = "android_activity_high_accuracy",
            )

            RootActivityMotion.MovingCandidate -> RootLocationPolicy(
                motion = motion,
                priority = Priority.PRIORITY_HIGH_ACCURACY,
                intervalMillis = 15_000L,
                minUpdateIntervalMillis = 5_000L,
                minUpdateDistanceMeters = 5f,
                waitForAccurateLocation = true,
                source = "android_activity_candidate",
            )

            RootActivityMotion.Unknown -> RootLocationPolicy(
                motion = motion,
                priority = Priority.PRIORITY_HIGH_ACCURACY,
                intervalMillis = 5 * 60_000L,
                minUpdateIntervalMillis = 5 * 60_000L,
                minUpdateDistanceMeters = 0f,
                waitForAccurateLocation = true,
                source = "android_activity_unknown_high_accuracy",
            )
        }

    private fun vehicleHighAccuracy(motion: RootActivityMotion): RootLocationPolicy =
        RootLocationPolicy(
            motion = motion,
            priority = Priority.PRIORITY_HIGH_ACCURACY,
            intervalMillis = 60_000L,
            minUpdateIntervalMillis = 60_000L,
            minUpdateDistanceMeters = 10f,
            waitForAccurateLocation = true,
            source = "android_activity_high_accuracy",
        )
}
