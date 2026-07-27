package kr.co.root.legolas.location.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationSampleRepositoryTest {
    @Test
    fun clearWaitsForInFlightInsertAndLeavesNoOrphanSample() = runBlocking {
        val dao = BlockingLocationSampleDao()
        val settings = LocationSettingsRepository(
            ApplicationProvider.getApplicationContext<Context>(),
        )
        val repository = LocationSampleRepository(dao, settings)
        repository.setTrackingEnabled(true)

        val enqueue = async(Dispatchers.Default) {
            repository.enqueue(
                LocationSampleRequest(
                    clientSampleId = "in-flight",
                    collectedAtMillis = 1_000L,
                    latitude = 37.0,
                    longitude = 127.0,
                    horizontalAccuracyM = 10.0,
                    source = "test",
                    activityType = null,
                ),
            )
        }
        dao.insertStarted.await()
        repository.setTrackingEnabled(false)
        val clear = async(Dispatchers.Default) { repository.clearQueue() }

        assertNull(withTimeoutOrNull(100) { dao.deleteStarted.await() })
        dao.allowInsert.complete(Unit)
        enqueue.await()
        clear.await()

        assertTrue(dao.rows.isEmpty())
    }

    private class BlockingLocationSampleDao : LocationSampleDao {
        val rows = linkedMapOf<String, LocationSampleEntity>()
        val insertStarted = CompletableDeferred<Unit>()
        val allowInsert = CompletableDeferred<Unit>()
        val deleteStarted = CompletableDeferred<Unit>()

        override suspend fun insert(sample: LocationSampleEntity) {
            insertStarted.complete(Unit)
            allowInsert.await()
            rows[sample.clientSampleId] = sample
        }

        override suspend fun count(): Int = rows.size

        override suspend fun oldest(limit: Int): List<LocationSampleEntity> =
            rows.values.sortedBy(LocationSampleEntity::collectedAtMillis).take(limit)

        override suspend fun delete(clientSampleIds: List<String>) {
            clientSampleIds.forEach(rows::remove)
        }

        override suspend fun deleteAll() {
            deleteStarted.complete(Unit)
            rows.clear()
        }
    }
}
