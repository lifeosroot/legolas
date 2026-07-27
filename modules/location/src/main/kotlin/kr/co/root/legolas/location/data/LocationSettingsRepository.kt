package kr.co.root.legolas.location.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.locationDataStore by preferencesDataStore(name = "location")

@Singleton
class LocationSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.locationDataStore

    val enabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[TRACKING_ENABLED] ?: false
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[TRACKING_ENABLED] = enabled }
    }

    private companion object {
        val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
    }
}
