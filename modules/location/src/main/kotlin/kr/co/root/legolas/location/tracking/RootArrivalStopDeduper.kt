package kr.co.root.legolas.location.tracking

internal class RootArrivalStopDeduper(
    private val windowMillis: Long = ArrivalDedupeWindowMillis,
    private val distanceMeters: Float = ArrivalDedupeDistanceMeters,
) {
    private var lastArrivalReading: RootLocationReading? = null

    fun shouldSave(reading: RootLocationReading, nowMillis: Long): Boolean {
        val previous = lastArrivalReading
        if (
            previous != null &&
            nowMillis - previous.timeMillis <= windowMillis &&
            previous.distanceMetersTo(reading) < distanceMeters
        ) {
            return false
        }
        lastArrivalReading = reading
        return true
    }

    private companion object {
        const val ArrivalDedupeWindowMillis = 5 * 60_000L
        const val ArrivalDedupeDistanceMeters = 100f
    }
}

