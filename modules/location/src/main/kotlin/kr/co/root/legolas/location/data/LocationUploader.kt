package kr.co.root.legolas.location.data

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kr.co.root.legolas.location.permission.hasLocationServerAccess
import java.net.HttpURLConnection
import java.net.URI
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationUploader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: LocationSampleRepository,
    private val serverConfigProvider: LocationServerConfigProvider,
) {
    private val mutex = Mutex()

    suspend fun flush() = mutex.withLock {
        while (repository.isTrackingEnabled()) {
            val samples = repository.oldest()
            if (samples.isEmpty()) {
                repository.setLastErrorMessage(null)
                return@withLock
            }
            val config = serverConfigProvider.current()
                ?: throw LocationUploadException(
                    message = "Arwen pairing is required before location samples can be uploaded.",
                    retryable = false,
                )
            if (!context.hasLocationServerAccess(config.serverUrl)) {
                throw LocationUploadException(
                    message = "로컬 Arwen 서버로 전송하려면 주변 네트워크 권한이 필요합니다.",
                    retryable = false,
                )
            }
            upload(config, samples)
            repository.removeUploaded(samples)
        }
    }

    private suspend fun upload(
        config: LocationServerConfig,
        samples: List<LocationSampleRequest>,
    ) = withContext(Dispatchers.IO) {
        val endpoint = URI.create(config.serverUrl.trimEnd('/') + "/api/location/samples/batch").toURL()
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = false
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            setRequestProperty("Content-Type", "application/json")
        }
        Log.i(LogTag, "LocationUpload start endpoint=$endpoint samples=${samples.size}")
        try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
                it.write(samples.toJson().toString())
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                val error = connection.errorStream
                    ?.bufferedReader()
                    ?.use { it.readText().take(500) }
                    .orEmpty()
                throw LocationUploadException(
                    message = "Arwen rejected location upload (HTTP $status): $error",
                    retryable = status == 408 || status == 429 || status >= 500,
                )
            }
            Log.i(
                LogTag,
                "LocationUpload success endpoint=$endpoint status=$status samples=${samples.size}",
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            Log.w(
                LogTag,
                "LocationUpload failed endpoint=$endpoint samples=${samples.size} " +
                    "retryable=${throwable.shouldRetryLocationUpload()}",
                throwable,
            )
            throw throwable
        } finally {
            connection.disconnect()
        }
    }

    private fun List<LocationSampleRequest>.toJson(): JSONObject =
        JSONObject().put(
            "samples",
            JSONArray().apply {
                forEach { sample ->
                    put(
                        JSONObject()
                            .put("clientSampleId", sample.clientSampleId)
                            .put("collectedAt", Instant.ofEpochMilli(sample.collectedAtMillis).toString())
                            .put("latitude", sample.latitude)
                            .put("longitude", sample.longitude)
                            .putNullable("horizontalAccuracyM", sample.horizontalAccuracyM)
                            .put("source", sample.source)
                            .putNullable("activityType", sample.activityType)
                            .putNullable("saveReason", sample.saveReason),
                    )
                }
            },
        )

    private fun JSONObject.putNullable(name: String, value: Any?): JSONObject =
        put(name, value ?: JSONObject.NULL)
}

private const val LogTag = "Legolas"
