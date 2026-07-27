package kr.co.root.legolas.location.tracking

internal class RootLocationSampleGate {
    private var lastAcceptedReading: RootLocationReading? = null

    fun shouldAccept(location: android.location.Location, motion: RootActivityMotion): Boolean {
        val reading = location.toRootLocationReading() ?: return false
        return shouldAccept(reading, motion)
    }

    fun shouldAccept(reading: RootLocationReading, motion: RootActivityMotion): Boolean {
        if (motion == RootActivityMotion.MovingCandidate || motion == RootActivityMotion.MovingDegraded) return false
        if (!reading.hasUsableSampleAccuracy()) return false

        val previous = lastAcceptedReading
        val shouldAccept = when {
            previous == null -> true
            reading.timeMillis <= previous.timeMillis -> false
            else -> reading.shouldAcceptAfter(previous, motion)
        }

        if (shouldAccept) {
            lastAcceptedReading = reading
        }
        return shouldAccept
    }

    private fun RootLocationReading.shouldAcceptAfter(
        previous: RootLocationReading,
        motion: RootActivityMotion,
    ): Boolean {
        val elapsedMillis = timeMillis - previous.timeMillis
        val minimumIntervalMillis = motion.minimumSampleIntervalMillis()
        if (elapsedMillis < minimumIntervalMillis) return false

        return when (motion) {
            RootActivityMotion.MovingCandidate -> false
            RootActivityMotion.MovingDegraded -> false
            RootActivityMotion.Moving -> {
                hasMeaningfulSampleDisplacement(previous) ||
                    elapsedMillis >= MovingHeartbeatIntervalMillis
            }

            RootActivityMotion.Still -> elapsedMillis >= StillHeartbeatIntervalMillis
            RootActivityMotion.Unknown -> {
                hasLargeSampleDisplacement(previous) ||
                    elapsedMillis >= UnknownHeartbeatIntervalMillis
            }
        }
    }

    private fun RootLocationReading.hasMeaningfulSampleDisplacement(previous: RootLocationReading): Boolean {
        val threshold = maxOf(
            MovingSampleDistanceMeters,
            maxOf(accuracyMeters ?: 0f, previous.accuracyMeters ?: 0f) * SampleAccuracyMultiplier,
        )
        return distanceMetersTo(previous) >= threshold
    }

    private fun RootLocationReading.hasLargeSampleDisplacement(previous: RootLocationReading): Boolean =
        distanceMetersTo(previous) >= UnknownSampleDistanceMeters

    private fun RootLocationReading.hasUsableSampleAccuracy(): Boolean =
        accuracyMeters != null &&
            accuracyMeters >= 0f &&
            accuracyMeters <= SampleMaxAccuracyMeters

    private fun RootActivityMotion.minimumSampleIntervalMillis(): Long =
        when (this) {
            RootActivityMotion.MovingCandidate -> MovingCandidateSampleIntervalMillis
            RootActivityMotion.MovingDegraded -> MovingDegradedSampleIntervalMillis
            RootActivityMotion.Moving -> MovingSampleIntervalMillis
            RootActivityMotion.Still -> StillSampleIntervalMillis
            RootActivityMotion.Unknown -> UnknownSampleIntervalMillis
        }

    private companion object {
        const val SampleMaxAccuracyMeters = 50f
        const val SampleAccuracyMultiplier = 1.5f
        const val MovingCandidateSampleIntervalMillis = 30_000L
        const val MovingDegradedSampleIntervalMillis = 30_000L
        const val MovingSampleIntervalMillis = 15_000L
        const val MovingSampleDistanceMeters = 25f
        const val MovingHeartbeatIntervalMillis = 60_000L
        const val StillSampleIntervalMillis = 15 * 60_000L
        const val StillHeartbeatIntervalMillis = 15 * 60_000L
        const val UnknownSampleIntervalMillis = 15 * 60_000L
        const val UnknownHeartbeatIntervalMillis = 15 * 60_000L
        const val UnknownSampleDistanceMeters = 100f
    }
}

