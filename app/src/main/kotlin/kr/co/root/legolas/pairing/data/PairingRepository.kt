package kr.co.root.legolas.pairing.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kr.co.root.legolas.pairing.model.PairingConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pairingDataStore by preferencesDataStore(name = "pairing")

@Singleton
class PairingRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.pairingDataStore

    val pairing: Flow<PairingConfig?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it.toPairingConfig() }
        .flowOn(Dispatchers.IO)

    suspend fun save(pairing: PairingConfig) {
        withContext(Dispatchers.IO) {
            val encryptedApiKey = ApiKeyCipher.encrypt(pairing.apiKey)
            dataStore.edit { preferences ->
                preferences[SERVER_URL] = pairing.serverUrl
                preferences[API_KEY] = encryptedApiKey
            }
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    internal suspend fun checkHealth(
        pairing: PairingConfig,
    ): PairingHealthResult = withContext(Dispatchers.IO) {
        try {
            val connection = (
                pairingHealthEndpoint(pairing.serverUrl).openConnection() as HttpURLConnection
            ).apply {
                    requestMethod = "GET"
                    connectTimeout = HEALTH_TIMEOUT_MILLIS
                    readTimeout = HEALTH_TIMEOUT_MILLIS
                    instanceFollowRedirects = false
                    setRequestProperty("Authorization", "Bearer ${pairing.apiKey}")
                    setRequestProperty("Accept", "application/json")
                }
            try {
                pairingHealthResult(connection.responseCode)
            } finally {
                connection.disconnect()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            PairingHealthResult.Unavailable
        }
    }

    private fun Preferences.toPairingConfig(): PairingConfig? {
        val serverUrl = this[SERVER_URL] ?: return null
        val encryptedApiKey = this[API_KEY] ?: return null
        return runCatching {
            PairingConfig(serverUrl, ApiKeyCipher.decrypt(encryptedApiKey))
        }.getOrNull()
    }

    private companion object {
        const val HEALTH_TIMEOUT_MILLIS = 10_000
        val SERVER_URL = stringPreferencesKey("server_url")
        val API_KEY = stringPreferencesKey("api_key")
    }
}

internal fun pairingHealthResult(statusCode: Int): PairingHealthResult = when (statusCode) {
    in 200..299 -> PairingHealthResult.Healthy
    HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN ->
        PairingHealthResult.Rejected
    else -> PairingHealthResult.Unavailable
}

internal fun pairingHealthEndpoint(serverUrl: String) =
    URI.create(serverUrl.trimEnd('/') + "/api/health").toURL()
