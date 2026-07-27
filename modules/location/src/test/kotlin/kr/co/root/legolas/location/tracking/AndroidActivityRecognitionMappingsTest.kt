package kr.co.root.legolas.location.tracking

import com.google.android.gms.location.DetectedActivity
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidActivityRecognitionMappingsTest {

    @Test
    fun pedestrianActivitiesMapToMovingCandidateAndRawLabels() {
        val walking = DetectedActivity(DetectedActivity.WALKING, 90)
        val running = DetectedActivity(DetectedActivity.RUNNING, 90)
        val vehicle = DetectedActivity(DetectedActivity.IN_VEHICLE, 90)

        assertEquals(RootActivityMotion.MovingCandidate, walking.toRootMotion())
        assertEquals("WALKING", walking.toRootActivityType())
        assertEquals(RootActivityMotion.MovingCandidate, running.toRootMotion())
        assertEquals("RUNNING", running.toRootActivityType())
        assertEquals(RootActivityMotion.Moving, vehicle.toRootMotion())
        assertEquals("IN_VEHICLE", vehicle.toRootActivityType())
    }

    @Test
    fun stillAndUnknownActivitiesKeepDistinctLabels() {
        val still = DetectedActivity(DetectedActivity.STILL, 90)
        val unknown = DetectedActivity(DetectedActivity.UNKNOWN, 20)

        assertEquals(RootActivityMotion.Still, still.toRootMotion())
        assertEquals("STILL", still.toRootActivityType())
        assertEquals(RootActivityMotion.Unknown, unknown.toRootMotion())
        assertEquals("UNKNOWN", unknown.toRootActivityType())
    }

    @Test
    fun probableActivitiesUseSummedPedestrianMovingScore() {
        val classification = listOf(
            DetectedActivity(DetectedActivity.WALKING, 20),
            DetectedActivity(DetectedActivity.RUNNING, 25),
            DetectedActivity(DetectedActivity.ON_FOOT, 10),
        ).toRootActivityClassification(
            currentMotion = RootActivityMotion.Still,
            currentActivityType = "STILL",
        )

        assertEquals(RootActivityMotion.MovingCandidate, classification.motion)
        assertEquals("RUNNING", classification.activityType)
        assertEquals(20, classification.activityConfidences["WALKING"])
        assertEquals(25, classification.activityConfidences["RUNNING"])
        assertEquals(10, classification.activityConfidences["ON_FOOT"])
    }

    @Test
    fun probableActivitiesIncludeBicycleInMovingScore() {
        val classification = listOf(
            DetectedActivity(DetectedActivity.ON_BICYCLE, 50),
            DetectedActivity(DetectedActivity.STILL, 30),
        ).toRootActivityClassification(
            currentMotion = RootActivityMotion.Still,
            currentActivityType = "STILL",
        )

        assertEquals(RootActivityMotion.MovingCandidate, classification.motion)
        assertEquals("ON_BICYCLE", classification.activityType)
    }

    @Test
    fun probableActivitiesClassifyVehicleAtThreshold() {
        val classification = listOf(
            DetectedActivity(DetectedActivity.IN_VEHICLE, 40),
            DetectedActivity(DetectedActivity.STILL, 30),
        ).toRootActivityClassification(
            currentMotion = RootActivityMotion.Still,
            currentActivityType = "STILL",
        )

        assertEquals(RootActivityMotion.Moving, classification.motion)
        assertEquals("IN_VEHICLE", classification.activityType)
    }

    @Test
    fun vehicleClassificationKeepsPedestrianConfidencesForReleaseGate() {
        val classification = listOf(
            DetectedActivity(DetectedActivity.IN_VEHICLE, 45),
            DetectedActivity(DetectedActivity.WALKING, 88),
            DetectedActivity(DetectedActivity.ON_FOOT, 88),
        ).toRootActivityClassification(
            currentMotion = RootActivityMotion.Moving,
            currentActivityType = "IN_VEHICLE",
        )

        assertEquals(RootActivityMotion.Moving, classification.motion)
        assertEquals("IN_VEHICLE", classification.activityType)
        assertEquals(88, classification.activityConfidences["WALKING"])
        assertEquals(88, classification.activityConfidences["ON_FOOT"])
    }

    @Test
    fun probableActivitiesClassifyStillAtThreshold() {
        val classification = listOf(
            DetectedActivity(DetectedActivity.STILL, 65),
            DetectedActivity(DetectedActivity.WALKING, 20),
        ).toRootActivityClassification(
            currentMotion = RootActivityMotion.Moving,
            currentActivityType = "WALKING",
        )

        assertEquals(RootActivityMotion.Still, classification.motion)
        assertEquals("STILL", classification.activityType)
    }

    @Test
    fun probableActivitiesKeepCurrentStateBelowThresholds() {
        val classification = listOf(
            DetectedActivity(DetectedActivity.WALKING, 20),
            DetectedActivity(DetectedActivity.RUNNING, 10),
            DetectedActivity(DetectedActivity.IN_VEHICLE, 30),
            DetectedActivity(DetectedActivity.STILL, 40),
            DetectedActivity(DetectedActivity.UNKNOWN, 80),
        ).toRootActivityClassification(
            currentMotion = RootActivityMotion.Moving,
            currentActivityType = "WALKING",
        )

        assertEquals(RootActivityMotion.Moving, classification.motion)
        assertEquals("WALKING", classification.activityType)
    }
}
