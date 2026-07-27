package kr.co.root.legolas.location.tracking

internal class RootMovingCandidateStartBuffer<T>(
    private val maxAgeMillis: Long = MaxAgeMillis,
    private val maxSize: Int = MaxSize,
    private val maxAccuracyMeters: Float = MaxAccuracyMeters,
) {
    private val entries = ArrayDeque<Entry<T>>()

    fun record(
        value: T,
        timeMillis: Long,
        accuracyMeters: Float?,
    ) {
        if (accuracyMeters == null || accuracyMeters < 0f || accuracyMeters > maxAccuracyMeters) return

        entries += Entry(
            value = value,
            timeMillis = timeMillis,
            accuracyMeters = accuracyMeters,
        )
        trim(nowMillis = timeMillis)
    }

    fun best(): T? =
        entries
            .filter { it.accuracyMeters <= PreferredMaxAccuracyMeters }
            .minByOrNull { it.timeMillis }
            ?.value
            ?: entries
                .minWithOrNull(
                    compareBy<Entry<T>> { it.accuracyMeters }
                        .thenBy { it.timeMillis },
                )
                ?.value

    fun clear() {
        entries.clear()
    }

    private fun trim(nowMillis: Long) {
        while (entries.isNotEmpty() && nowMillis - entries.first().timeMillis > maxAgeMillis) {
            entries.removeFirst()
        }
        while (entries.size > maxSize) {
            entries.removeFirst()
        }
    }

    private data class Entry<T>(
        val value: T,
        val timeMillis: Long,
        val accuracyMeters: Float,
    )

    private companion object {
        const val MaxAgeMillis = 2 * 60_000L
        const val MaxSize = 12
        const val MaxAccuracyMeters = 50f
        const val PreferredMaxAccuracyMeters = 35f
    }
}

