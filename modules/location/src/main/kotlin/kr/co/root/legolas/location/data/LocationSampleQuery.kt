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
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

enum class LocationSampleQuality {
    GOOD,
    FAIR,
    BAD,
}

data class LocationSample(
    val id: Long,
    val collectedAt: Instant,
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyM: Double?,
    val source: String,
    val quality: LocationSampleQuality,
    val activityType: String?,
    val saveReason: String?,
)

@Singleton
class LocationSampleQuery @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val serverConfigProvider: LocationServerConfigProvider,
) {
    suspend fun samplesOn(
        date: LocalDate,
        quality: LocationSampleQuality?,
    ): List<LocationSample> = withContext(Dispatchers.IO) {
        val config = serverConfigProvider.current()
            ?: throw IOException("Arwen pairing is required")
        if (!context.hasLocationServerAccess(config.serverUrl)) {
            throw IOException("Local network access is required")
        }

        val endpoint = URI.create(
            config.serverUrl.trimEnd('/') + locationSamplesPath(date, quality),
        ).toURL()
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = false
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("Arwen returned HTTP $status")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                JSONArray(reader.readText()).toLocationSamples()
            }
        } finally {
            connection.disconnect()
        }
    }
}

internal fun locationSamplesPath(
    date: LocalDate,
    quality: LocationSampleQuality?,
): String = buildString {
    append("/api/location/samples?date=")
    append(date)
    quality?.let {
        append("&quality=")
        append(it.name)
    }
}

private fun JSONArray.toLocationSamples(): List<LocationSample> =
    List(length()) { index -> getJSONObject(index).toLocationSample() }

private fun JSONObject.toLocationSample(): LocationSample = LocationSample(
    id = getLong("id"),
    collectedAt = Instant.parse(getString("collectedAt")),
    latitude = getDouble("latitude"),
    longitude = getDouble("longitude"),
    horizontalAccuracyM = nullableDouble("horizontalAccuracyM"),
    source = getString("source"),
    quality = LocationSampleQuality.valueOf(getString("quality")),
    activityType = nullableString("activityType"),
    saveReason = nullableString("saveReason"),
)

private fun JSONObject.nullableDouble(name: String): Double? =
    if (isNull(name)) null else getDouble(name)

private fun JSONObject.nullableString(name: String): String? =
    if (isNull(name)) null else getString(name)
