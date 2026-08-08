package kr.co.root.legolas.location.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.co.root.legolas.location.permission.hasLocationServerAccess
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

data class LocationPlace(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
)

data class LocationPlaceDraft(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
)

@Singleton
class LocationPlaceRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val serverConfigProvider: LocationServerConfigProvider,
) {
    suspend fun findAll(): List<LocationPlace> =
        JSONArray(request("GET", PlacesPath)).toLocationPlaces()

    suspend fun create(draft: LocationPlaceDraft): LocationPlace =
        JSONObject(request("POST", PlacesPath, draft.toRequestJson())).toLocationPlace()

    suspend fun update(id: Long, draft: LocationPlaceDraft): LocationPlace =
        JSONObject(request("PUT", "$PlacesPath/$id", draft.toRequestJson())).toLocationPlace()

    suspend fun delete(id: Long) {
        request("DELETE", "$PlacesPath/$id")
    }

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
    ): String = withContext(Dispatchers.IO) {
        val config = serverConfigProvider.current()
            ?: throw IOException("Arwen pairing is required")
        if (!context.hasLocationServerAccess(config.serverUrl)) {
            throw IOException("Local network access is required")
        }

        val endpoint = URI.create(config.serverUrl.trimEnd('/') + path).toURL()
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = false
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            body?.let { requestBody ->
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
                    it.write(requestBody.toString())
                }
            }
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("Arwen returned HTTP $status")
            if (status == HttpURLConnection.HTTP_NO_CONTENT) {
                ""
            } else {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        } finally {
            connection.disconnect()
        }
    }
}

internal fun LocationPlaceDraft.toRequestJson(): JSONObject =
    JSONObject()
        .put("name", name.trim())
        .put("latitude", latitude)
        .put("longitude", longitude)
        .put("radiusMeters", radiusMeters)

internal fun JSONArray.toLocationPlaces(): List<LocationPlace> =
    List(length()) { index -> getJSONObject(index).toLocationPlace() }

private fun JSONObject.toLocationPlace(): LocationPlace = LocationPlace(
    id = getLong("id"),
    name = getString("name"),
    latitude = getDouble("latitude"),
    longitude = getDouble("longitude"),
    radiusMeters = getDouble("radiusMeters"),
)

private const val PlacesPath = "/api/location/places"
