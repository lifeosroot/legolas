package kr.co.root.legolas.location.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.locationDataStore by preferencesDataStore(name = "location")

data class LocationTrackingState(
    val enabled: Boolean = false,
    val serviceRunning: Boolean = false,
    val motion: String = "unknown",
    val queuedCount: Int = 0,
    val lastCollectedAtMillis: Long? = null,
    val lastError: String? = null,
)

@Singleton
class LocationSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.locationDataStore

    val state: Flow<LocationTrackingState> = dataStore.data.map { it.toTrackingState() }
    val enabled: Flow<Boolean> = state.map { it.enabled }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit {
            it[TRACKING_ENABLED] = enabled
            if (!enabled) it[SERVICE_RUNNING] = false
        }
    }

    suspend fun setServiceRunning(running: Boolean) {
        dataStore.edit { it[SERVICE_RUNNING] = running }
    }

    suspend fun setMotion(motion: String) {
        dataStore.edit { it[MOTION] = motion }
    }

    suspend fun recordCollected(atMillis: Long, queuedCount: Int) {
        dataStore.edit {
            val previous = it[LAST_COLLECTED_AT]
            it[LAST_COLLECTED_AT] = latestCollectedAt(previous, atMillis)
            it[QUEUED_COUNT] = queuedCount.coerceAtLeast(0)
        }
    }

    suspend fun setQueuedCount(count: Int) {
        dataStore.edit { it[QUEUED_COUNT] = count.coerceAtLeast(0) }
    }

    suspend fun setLastError(message: String?) {
        dataStore.edit {
            if (message.isNullOrBlank()) it.remove(LAST_ERROR) else it[LAST_ERROR] = message
        }
    }

    private fun Preferences.toTrackingState() = LocationTrackingState(
        enabled = this[TRACKING_ENABLED] ?: false,
        serviceRunning = this[SERVICE_RUNNING] ?: false,
        motion = this[MOTION] ?: "unknown",
        queuedCount = this[QUEUED_COUNT] ?: 0,
        lastCollectedAtMillis = this[LAST_COLLECTED_AT],
        lastError = this[LAST_ERROR],
    )

    private companion object {
        val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
        val SERVICE_RUNNING = booleanPreferencesKey("service_running")
        val MOTION = stringPreferencesKey("motion")
        val QUEUED_COUNT = intPreferencesKey("queued_count")
        val LAST_COLLECTED_AT = longPreferencesKey("last_collected_at")
        val LAST_ERROR = stringPreferencesKey("last_error")
    }
}

internal fun latestCollectedAt(previous: Long?, candidate: Long): Long =
    previous?.coerceAtLeast(candidate) ?: candidate
