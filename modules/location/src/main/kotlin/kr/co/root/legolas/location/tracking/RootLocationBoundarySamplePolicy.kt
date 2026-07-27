package kr.co.root.legolas.location.tracking

import java.time.Instant

internal object RootLocationBoundarySamplePolicy {
    const val MoveStartSaveReason = "MOVE_START"
    const val ArrivalSaveReason = "ARRIVAL"
    const val MotionTransitionSaveReason = "MOTION_TRANSITION"

    fun shouldPersist(
        collectedAt: Instant,
        lastAcceptedAt: Instant?,
        saveReason: String?,
    ): Boolean =
        isBoundarySaveReason(saveReason) ||
            lastAcceptedAt == null ||
            collectedAt.isAfter(lastAcceptedAt)

    fun nextLastAcceptedAt(
        current: Instant?,
        collectedAt: Instant,
        saveReason: String?,
    ): Instant =
        if (isBoundarySaveReason(saveReason) && current != null && !collectedAt.isAfter(current)) {
            current
        } else {
            collectedAt
        }

    private fun isBoundarySaveReason(saveReason: String?): Boolean =
        saveReason == MoveStartSaveReason || saveReason == ArrivalSaveReason
}

