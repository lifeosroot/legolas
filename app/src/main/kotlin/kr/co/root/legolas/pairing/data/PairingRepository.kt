package kr.co.root.legolas.pairing.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kr.co.root.legolas.pairing.model.PairingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

private val Context.pairingDataStore by preferencesDataStore(name = "pairing")

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

    private fun Preferences.toPairingConfig(): PairingConfig? {
        val serverUrl = this[SERVER_URL] ?: return null
        val encryptedApiKey = this[API_KEY] ?: return null
        return runCatching {
            PairingConfig(serverUrl, ApiKeyCipher.decrypt(encryptedApiKey))
        }.getOrNull()
    }

    private companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val API_KEY = stringPreferencesKey("api_key")
    }
}
