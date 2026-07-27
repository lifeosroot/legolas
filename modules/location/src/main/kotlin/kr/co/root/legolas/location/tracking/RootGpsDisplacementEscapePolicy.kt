package kr.co.root.legolas.location.tracking

internal object RootGpsDisplacementEscapePolicy {
    fun shouldEscape(
        currentMotion: RootActivityMotion,
        evaluation: RootLocationMovementEvaluation,
    ): Boolean {
        if (currentMotion != RootActivityMotion.Still && currentMotion != RootActivityMotion.Unknown) {
            return false
        }
        if (!evaluation.accepted) return false
        val distanceMeters = evaluation.distanceMeters ?: return false
        val accuracyMeters = evaluation.accuracyMeters ?: return false
        val requiredDistanceMeters = evaluation.requiredDistanceMeters ?: return false
        if (accuracyMeters > MaxEscapeAccuracyMeters) return false

        val escapeDistanceMeters = maxOf(
            MinEscapeDistanceMeters,
            requiredDistanceMeters * RequiredDistanceMultiplier,
        )
        return distanceMeters >= escapeDistanceMeters
    }

    private const val MaxEscapeAccuracyMeters = 50f
    private const val MinEscapeDistanceMeters = 300f
    private const val RequiredDistanceMultiplier = 4f
}

