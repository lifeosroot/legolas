package kr.co.root.legolas.location.tracking

import kotlin.math.ceil

internal class RootLocationMotionGate {
    private var lastReading: RootLocationReading? = null
    private val recentReadings = ArrayDeque<RootLocationReading>()
    private val recentMovementSignalTimes = ArrayDeque<Long>()
    private var consecutiveMovementSignalCount = 0
    private var lastMeaningfulMovementAtMillis: Long? = null

    fun onLocation(location: android.location.Location): RootActivityMotion? {
        return onLocationWithDiagnostics(location).motion
    }

    fun onLocationWithDiagnostics(location: android.location.Location): RootLocationMotionResult {
        val reading = location.toRootLocationReading()
        return if (reading == null) {
            RootLocationMotionResult(
                motion = null,
                evaluation = RootLocationMovementEvaluation(
                    reading = null,
                    distanceMeters = null,
                    accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                    requiredDistanceMeters = null,
                    accepted = false,
                    reason = RootLocationMovementDecisionReason.INVALID_LOCATION,
                    movementEvidenceScore = recentMovementSignalTimes.size,
                    consecutiveMovementSignals = consecutiveMovementSignalCount,
                ),
            )
        } else {
            onReadingWithDiagnostics(reading)
        }
    }

    fun onReading(reading: RootLocationReading): RootActivityMotion? {
        return onReadingWithDiagnostics(reading).motion
    }

    fun onReadingWithDiagnostics(reading: RootLocationReading): RootLocationMotionResult {
        pruneMovementSignals(reading.timeMillis)
        val previous = lastReading
        if (!reading.hasUsableMotionAccuracy()) {
            return RootLocationMotionResult(
                motion = null,
                evaluation = reading.toMovementEvaluation(
                    previous = previous,
                    accepted = false,
                    reason = RootLocationMovementDecisionReason.ACCURACY_TOO_POOR,
                ),
            )
        }
        if (previous != null && reading.timeMillis <= previous.timeMillis) {
            return RootLocationMotionResult(
                motion = null,
                evaluation = reading.toMovementEvaluation(
                    previous = previous,
                    accepted = false,
                    reason = RootLocationMovementDecisionReason.TIMESTAMP_NOT_INCREASING,
                ),
            )
        }

        recordReading(reading)
        val hasMovingSignal = reading.hasMovingSignal(previous)
        lastReading = reading

        if (hasMovingSignal) {
            lastMeaningfulMovementAtMillis = reading.timeMillis
            recentMovementSignalTimes += reading.timeMillis
            pruneMovementSignals(reading.timeMillis)
            consecutiveMovementSignalCount += 1
            return RootLocationMotionResult(
                motion = if (recentMovementSignalTimes.size >= MovingEvidenceThreshold) {
                    RootActivityMotion.Moving
                } else {
                    null
                },
                evaluation = reading.toMovementEvaluation(
                    previous = previous,
                    accepted = true,
                    reason = RootLocationMovementDecisionReason.MOVEMENT_SIGNAL,
                ),
            )
        } else {
            consecutiveMovementSignalCount = 0
        }

        val motion = if (hasStableStillWindow()) {
            RootActivityMotion.Still
        } else {
            null
        }
        return RootLocationMotionResult(
            motion = motion,
            evaluation = reading.toMovementEvaluation(
                previous = previous,
                accepted = false,
                reason = if (previous == null) {
                    RootLocationMovementDecisionReason.FIRST_SAMPLE
                } else {
                    RootLocationMovementDecisionReason.DISTANCE_TOO_SMALL
                },
            ),
        )
    }

    fun lastMeaningfulMovementAtMillis(): Long? = lastMeaningfulMovementAtMillis

    fun recentMovementWindow(
        nowMillis: Long,
        windowMillis: Long,
        maxAccuracyMeters: Float = MotionEvidenceMaxAccuracyMeters,
    ): RootLocationMovementWindow {
        val windowReadings = recentReadings
            .filter {
                it.timeMillis <= nowMillis &&
                    nowMillis - it.timeMillis <= windowMillis &&
                    it.hasUsableMotionAccuracy(maxAccuracyMeters)
            }

        if (windowReadings.isEmpty()) {
            return RootLocationMovementWindow(
                sampleCount = 0,
                spanMillis = 0L,
                distanceMeters = 0f,
                startedAtMillis = null,
                endedAtMillis = null,
            )
        }

        val distanceMeters = windowReadings
            .zipWithNext()
            .sumOf { (previous, next) -> previous.distanceMetersTo(next).toDouble() }
            .toFloat()

        return RootLocationMovementWindow(
            sampleCount = windowReadings.size,
            spanMillis = windowReadings.last().timeMillis - windowReadings.first().timeMillis,
            distanceMeters = distanceMeters,
            startedAtMillis = windowReadings.first().timeMillis,
            endedAtMillis = windowReadings.last().timeMillis,
        )
    }

    fun hasStableStillWindow(nowMillis: Long? = null): Boolean {
        return stableWindowReadings(
            windowMillis = StillWindowMillis,
            minimumSampleCount = StillMinimumSampleCount,
            radiusMeters = StillP95RadiusMeters,
            nowMillis = nowMillis,
        ) != null
    }

    fun stableStillWindowStartMillis(nowMillis: Long? = null): Long? {
        return stableWindowReadings(
            windowMillis = StillWindowMillis,
            minimumSampleCount = StillMinimumSampleCount,
            radiusMeters = StillP95RadiusMeters,
            nowMillis = nowMillis,
        )?.firstOrNull()?.timeMillis
    }

    fun hasCandidateStillWindow(nowMillis: Long? = null, sinceMillis: Long? = null): Boolean {
        return stableWindowReadings(
            windowMillis = MovingCandidateStillWindowMillis,
            minimumSampleCount = MovingCandidateStillMinimumSampleCount,
            radiusMeters = MovingCandidateStillP95RadiusMeters,
            nowMillis = nowMillis,
            sinceMillis = sinceMillis,
        ) != null
    }

    fun recentMovementSignalCount(
        nowMillis: Long,
        windowMillis: Long = MovingEvidenceWindowMillis,
    ): Int {
        pruneMovementSignals(nowMillis)
        return recentMovementSignalTimes.count { nowMillis - it <= windowMillis }
    }

    private fun stableWindowReadings(
        windowMillis: Long,
        minimumSampleCount: Int,
        radiusMeters: Float,
        nowMillis: Long? = null,
        sinceMillis: Long? = null,
    ): List<RootLocationReading>? {
        val latest = recentReadings.lastOrNull() ?: return null
        if (nowMillis != null && nowMillis - latest.timeMillis > windowMillis) return null

        val windowReadings = recentReadings
            .filter {
                latest.timeMillis - it.timeMillis <= windowMillis &&
                    (sinceMillis == null || it.timeMillis >= sinceMillis)
            }
        if (windowReadings.size < minimumSampleCount) return null
        if (latest.timeMillis - windowReadings.first().timeMillis < minimumCoverageMillis(windowMillis)) {
            return null
        }

        val centerLatitude = windowReadings.map { it.latitude }.average()
        val centerLongitude = windowReadings.map { it.longitude }.average()
        val center = RootLocationReading(
            timeMillis = latest.timeMillis,
            latitude = centerLatitude,
            longitude = centerLongitude,
            accuracyMeters = 0f,
            speedMps = null,
        )
        val p95DistanceMeters = windowReadings
            .map { it.distanceMetersTo(center) }
            .percentile95()
        return windowReadings.takeIf { p95DistanceMeters <= radiusMeters }
    }

    fun confirmStill() {
        recentMovementSignalTimes.clear()
        consecutiveMovementSignalCount = 0
    }

    private fun recordReading(reading: RootLocationReading) {
        recentReadings += reading
        while (
            recentReadings.isNotEmpty() &&
            reading.timeMillis - recentReadings.first().timeMillis > StillWindowMillis
        ) {
            recentReadings.removeFirst()
        }
    }

    private fun pruneMovementSignals(nowMillis: Long) {
        while (
            recentMovementSignalTimes.isNotEmpty() &&
            nowMillis - recentMovementSignalTimes.first() > MovingEvidenceWindowMillis
        ) {
            recentMovementSignalTimes.removeFirst()
        }
    }

    private fun RootLocationReading.hasMovingSignal(previous: RootLocationReading?): Boolean {
        if (previous == null || timeMillis <= previous.timeMillis || !previous.hasUsableMotionAccuracy()) {
            return false
        }

        return distanceMetersTo(previous) >= movementDistanceThreshold(previous)
    }

    private fun RootLocationReading.toMovementEvaluation(
        previous: RootLocationReading?,
        accepted: Boolean,
        reason: RootLocationMovementDecisionReason,
    ): RootLocationMovementEvaluation {
        val distanceMeters = previous
            ?.takeIf { timeMillis > it.timeMillis }
            ?.let { distanceMetersTo(it) }
        return RootLocationMovementEvaluation(
            reading = this,
            distanceMeters = distanceMeters,
            accuracyMeters = accuracyMeters,
            requiredDistanceMeters = previous
                ?.takeIf { timeMillis > it.timeMillis && it.hasUsableMotionAccuracy() }
                ?.let { movementDistanceThreshold(it) },
            accepted = accepted,
            reason = reason,
            movementEvidenceScore = recentMovementSignalTimes.size,
            consecutiveMovementSignals = consecutiveMovementSignalCount,
        )
    }

    private fun RootLocationReading.movementDistanceThreshold(previous: RootLocationReading): Float =
        maxOf(
            MovingDistanceThresholdMeters,
            maxOf(accuracyMeters ?: 0f, previous.accuracyMeters ?: 0f) * MovingAccuracyMultiplier,
        )

    private fun RootLocationReading.hasUsableMotionAccuracy(
        maxAccuracyMeters: Float = MotionEvidenceMaxAccuracyMeters,
    ): Boolean =
        accuracyMeters != null &&
            accuracyMeters >= 0f &&
            accuracyMeters <= maxAccuracyMeters

    private fun minimumCoverageMillis(windowMillis: Long): Long =
        windowMillis * StableWindowMinimumCoveragePercent / 100

    private fun List<Float>.percentile95(): Float {
        if (isEmpty()) return Float.MAX_VALUE
        val sorted = sorted()
        val index = (ceil(sorted.size * 0.95).toInt() - 1).coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }

    private companion object {
        const val MovingEvidenceThreshold = 3
        const val MovingEvidenceWindowMillis = 3 * 60_000L
        const val MovingCandidateStillWindowMillis = 2 * 60_000L
        const val MovingCandidateStillMinimumSampleCount = 3
        const val MovingCandidateStillP95RadiusMeters = 80f
        const val StillWindowMillis = 5 * 60_000L
        const val StillMinimumSampleCount = 3
        const val StillP95RadiusMeters = 30f
        const val MovingDistanceThresholdMeters = 25f
        const val MotionEvidenceMaxAccuracyMeters = 50f
        const val MovingAccuracyMultiplier = 1.5f
        const val StableWindowMinimumCoveragePercent = 80L
    }
}
