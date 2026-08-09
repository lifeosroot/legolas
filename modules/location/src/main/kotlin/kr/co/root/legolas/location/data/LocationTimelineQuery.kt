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

@Singleton
class LocationTimelineQuery @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val serverConfigProvider: LocationServerConfigProvider,
) {
    suspend fun entriesOn(date: LocalDate): List<LocationTimelineEntry> = withContext(Dispatchers.IO) {
        val config = serverConfigProvider.current()
            ?: throw IOException("Arwen pairing is required")
        if (!context.hasLocationServerAccess(config.serverUrl)) {
            throw IOException("Local network access is required")
        }

        val endpoint = URI.create(
            config.serverUrl.trimEnd('/') + locationTimelinePath(date),
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
                JSONArray(reader.readText()).toLocationTimelineEntries()
            }
        } finally {
            connection.disconnect()
        }
    }
}

internal fun locationTimelinePath(date: LocalDate): String =
    "/api/location/timeline?date=$date"

internal fun JSONArray.toLocationTimelineEntries(): List<LocationTimelineEntry> =
    List(length()) { index -> getJSONObject(index).toLocationTimelineEntry() }

private fun JSONObject.toLocationTimelineEntry(): LocationTimelineEntry =
    LocationTimelineEntry(
        entryType = LocationTimelineEntryType.valueOf(getString("entryType")),
        startedAt = Instant.parse(getString("startedAt")),
        endedAt = nullableInstant("endedAt"),
        placeName = nullableString("placeName"),
        fromPlaceName = nullableString("fromPlaceName"),
        toPlaceName = nullableString("toPlaceName"),
        algorithmVersion = getString("algorithmVersion"),
    )

private fun JSONObject.nullableInstant(name: String): Instant? =
    nullableString(name)?.let(Instant::parse)

private fun JSONObject.nullableString(name: String): String? =
    if (isNull(name)) null else getString(name)
