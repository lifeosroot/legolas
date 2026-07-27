package kr.co.root.legolas.location.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class LocationSampleRepository @Inject constructor(
    private val dao: LocationSampleDao,
    private val settings: LocationSettingsRepository,
) {
    private val mutationMutex = Mutex()

    val trackingState: Flow<LocationTrackingState> = settings.state

    suspend fun setTrackingEnabled(enabled: Boolean) = settings.setEnabled(enabled)

    suspend fun setServiceRunning(running: Boolean) = settings.setServiceRunning(running)

    suspend fun setMotionState(motion: String) = settings.setMotion(motion)

    suspend fun setLastErrorMessage(message: String?) = settings.setLastError(message)

    suspend fun enqueue(sample: LocationSampleRequest) = mutationMutex.withLock {
        if (!settings.enabled.first()) return@withLock
        dao.insert(sample.asEntity())
        settings.recordCollected(sample.collectedAtMillis, dao.count())
    }

    suspend fun refreshQueueCount() = mutationMutex.withLock {
        settings.setQueuedCount(dao.count())
    }

    suspend fun isTrackingEnabled(): Boolean = settings.enabled.first()

    suspend fun oldest(limit: Int = 200): List<LocationSampleRequest> =
        dao.oldest(limit.coerceIn(1, 200)).map(LocationSampleEntity::asRequest)

    suspend fun removeUploaded(samples: List<LocationSampleRequest>) = mutationMutex.withLock {
        if (samples.isNotEmpty()) dao.delete(samples.map { it.clientSampleId })
        settings.setQueuedCount(dao.count())
    }

    suspend fun clearQueue() = mutationMutex.withLock {
        dao.deleteAll()
        settings.setQueuedCount(0)
    }
}
