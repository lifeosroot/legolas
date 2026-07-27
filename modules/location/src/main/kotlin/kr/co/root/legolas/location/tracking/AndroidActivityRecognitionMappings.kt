package kr.co.root.legolas.location.tracking

import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

internal const val AndroidActivityTypeUnknown = "UNKNOWN"

internal data class RootActivityClassification(
    val motion: RootActivityMotion,
    val activityType: String,
    val activityConfidences: Map<String, Int>,
)

internal fun ActivityRecognitionResult.toRootActivityClassification(
    currentMotion: RootActivityMotion,
    currentActivityType: String,
): RootActivityClassification =
    probableActivities.toRootActivityClassification(currentMotion, currentActivityType)

internal fun List<DetectedActivity>.toRootActivityClassification(
    currentMotion: RootActivityMotion,
    currentActivityType: String,
): RootActivityClassification {
    val movingScore = sumConfidence(MovingActivityTypes)
    val vehicleConfidence = confidenceFor(DetectedActivity.IN_VEHICLE)
    val stillConfidence = confidenceFor(DetectedActivity.STILL)
    val activityConfidences = confidenceByActivityType()

    return when {
        vehicleConfidence >= VehicleConfidenceThreshold -> RootActivityClassification(
            motion = RootActivityMotion.Moving,
            activityType = "IN_VEHICLE",
            activityConfidences = activityConfidences,
        )

        movingScore >= MovingScoreThreshold -> RootActivityClassification(
            motion = RootActivityMotion.MovingCandidate,
            activityType = bestActivityType(MovingActivityTypes),
            activityConfidences = activityConfidences,
        )

        stillConfidence >= StillConfidenceThreshold -> RootActivityClassification(
            motion = RootActivityMotion.Still,
            activityType = "STILL",
            activityConfidences = activityConfidences,
        )

        else -> RootActivityClassification(
            motion = currentMotion,
            activityType = currentActivityType,
            activityConfidences = activityConfidences,
        )
    }
}

internal fun DetectedActivity.toRootMotion(): RootActivityMotion =
    when (type) {
        DetectedActivity.WALKING,
        DetectedActivity.RUNNING,
        DetectedActivity.ON_BICYCLE,
        DetectedActivity.ON_FOOT,
        -> RootActivityMotion.MovingCandidate

        DetectedActivity.IN_VEHICLE -> RootActivityMotion.Moving

        DetectedActivity.STILL -> RootActivityMotion.Still
        else -> RootActivityMotion.Unknown
    }

internal fun DetectedActivity.toRootActivityType(): String =
    when (type) {
        DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
        DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
        DetectedActivity.ON_FOOT -> "ON_FOOT"
        DetectedActivity.RUNNING -> "RUNNING"
        DetectedActivity.STILL -> "STILL"
        DetectedActivity.TILTING -> "TILTING"
        DetectedActivity.WALKING -> "WALKING"
        DetectedActivity.UNKNOWN -> AndroidActivityTypeUnknown
        else -> AndroidActivityTypeUnknown
    }

private fun List<DetectedActivity>.sumConfidence(types: Set<Int>): Int =
    filter { it.type in types }.sumOf { it.confidence }

private fun List<DetectedActivity>.confidenceFor(type: Int): Int =
    filter { it.type == type }.maxOfOrNull { it.confidence } ?: 0

private fun List<DetectedActivity>.bestActivityType(types: Set<Int>): String =
    filter { it.type in types }
        .maxByOrNull { it.confidence }
        ?.toRootActivityType()
        ?: AndroidActivityTypeUnknown

private fun List<DetectedActivity>.confidenceByActivityType(): Map<String, Int> =
    groupBy { it.toRootActivityType() }
        .mapValues { (_, activities) -> activities.maxOf { it.confidence } }
        .filterValues { it > 0 }
        .toSortedMap()

private val MovingActivityTypes = setOf(
    DetectedActivity.WALKING,
    DetectedActivity.RUNNING,
    DetectedActivity.ON_FOOT,
    DetectedActivity.ON_BICYCLE,
)

private const val MovingScoreThreshold = 50
private const val VehicleConfidenceThreshold = 40
private const val StillConfidenceThreshold = 65

